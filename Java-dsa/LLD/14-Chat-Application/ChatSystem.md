# 14 · Chat Application, like WhatsApp (explained simply)

| | |
|---|---|
| **Code** | [ChatSystem.java](ChatSystem.java) |
| **Run** | `java ChatSystem.java` (26 self-checks, including 16 concurrent senders in one group) |
| **Design notes** | [README.md](README.md) (WebSockets, fan-out at scale, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

A chat app looks simple until you hit the two hard problems: **order** (phones' clocks disagree and networks reorder messages) and **delivery** (phones go offline constantly). This design solves both with two small ideas: a **server sequence number per conversation**, and **one cursor per member**.

---

## 1. The problem in plain words

Users chat one-to-one (**direct**) or in **groups**. They need:
- messages in the **same order** on every phone;
- a resent message (because the "sent" acknowledgement got lost) must **not appear twice**;
- someone offline gets everything they missed when they come back;
- **ticks**: one tick = sent, two ticks = delivered, blue ticks = read (in a group, only when **everyone** has read it);
- group admins can add and remove people; a new member doesn't see older history;
- "delete for everyone".

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Direct and group chats? | Both |
| Ordering? | One order per conversation that everyone agrees on |
| Retries? | The app retries if it doesn't get an ack; the message must not be duplicated |
| Offline users? | Messages are stored; the device syncs on reconnect |
| Receipts? | Sent / delivered / read; for groups, "read" means everyone read it |
| Group permissions? | Admins add and remove members |
| History for new members? | Only messages from after they joined |
| Media, end-to-end encryption, typing indicators? | Out of scope |

### Step 2 · Requirements
1. Send a message → the server assigns its **sequence number** (1, 2, 3, … per conversation).
2. A retried send (same client message id) returns the same message.
3. Online members get it pushed; offline members pull it later ("give me everything after seq N").
4. The device shows messages strictly in seq order, even if they arrive out of order.
5. Receipts from per-member cursors.
6. Admin-only membership changes; join-time history limits; delete for everyone.
7. Thread-safe with many senders in one group.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| user | `record User` |
| conversation | `class Conversation` (type, members, the message log, lock) |
| member | `class Member` (role, **deliveredUpTo**, **readUpTo**, joinedAtSeq) |
| message | `record Message(id, conversationId, seq, senderId, clientMsgId, text, serverTime, deleted)` |
| receipt | `enum Receipt { SENT, DELIVERED, READ }` |
| a connected phone | `interface DeviceConnection { void push(Message m); }` |
| the client app | `class ClientDevice` (hold-back buffer) |
| the server | `class ChatService` |

### Step 4 · Relationships

```
ChatService
   ├── Conversation(s) ── members (Member: role + 2 cursors) + log (List<Message>, index = seq − 1)
   │                    └── byClientId (dedupe: sender/clientMsgId → message)
   └── online: userId → DeviceConnection   (presence)
ClientDevice (on each phone): timeline + pending buffer
```

### Step 5 · API

```java
Conversation direct(a, b) / group(id, creator, others...)
void addMember / removeMember(convId, byUser, target)        // admins only
Message send(convId, sender, clientMsgId, text)
List<Message> sync(convId, userId, afterSeq)
void ackDelivered / ackRead(convId, userId, upToSeq)
Receipt receipt(convId, seq)
void deleteForEveryone(convId, user, seq)
```

### Step 6 · Patterns

| What matters | Pattern / idea |
|---|---|
| pushing to online devices | **Observer** (`DeviceConnection.push`) |
| retried sends | **idempotency key** (`clientMsgId`) |
| a total order per conversation | a **server-assigned sequence** under the conversation's lock |
| offline catch-up | **cursor-based sync** |
| delete without shifting the order | a **tombstone** |

### Step 7 · Concurrency
The contended resource is **one conversation's log**: assigning the next seq and appending must be atomic, so each conversation has its own lock.

---

## 3. The two key ideas, simply

### Idea 1: order by a server sequence number, never by phone time
Phone clocks can be minutes apart, and networks deliver packets out of order. So the **server** stamps each message: seq 1, 2, 3, … per conversation, under that conversation's lock. Every phone displays messages by seq, so everyone sees the **same** order.

### Idea 2: one cursor per member, not a status per message
Don't store "Bob received message 1, message 2, message 3…". For each (member, conversation), store just **two numbers**:
- `deliveredUpTo` = "Bob's phone has every message up to seq N";
- `readUpTo` = "Bob has read up to seq M".

Then:
- **sync** = "send me seq > N";
- **ticks** for message k = compare k with every member's cursors;
- **unread count** = last seq − readUpTo.

However many messages there are, it's always two numbers per member.

---

## 4. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `record` with a method returning a new record | `Message.tombstone()` | "a copy of me with the text removed" |
| `ReentrantLock` per conversation | `Conversation.lock` | conversations don't block each other |
| `AtomicLong.accumulateAndGet(x, Math::max)` | cursors | "set it to the larger of the current value and x", atomically: cursors only move forward |
| `computeIfAbsent` with a deterministic id | `direct(a, b)` | A→B and B→A find the same conversation |
| `List.copyOf(log.subList(...))` | `sync` | an immutable snapshot of the new messages |
| `TreeMap<Long, Message>` | the client's pending buffer | sorted by seq, so "is the next one here?" is easy |
| `synchronized` methods | `ClientDevice` | pushes and syncs can arrive from different threads |

---

## 5. The code, piece by piece

### 5.1 Direct chats get one shared id

```java
Conversation direct(String a, String b) {
    String id = "dm:" + (a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a);
    return conversations.computeIfAbsent(id, k -> { ... members a and b ... });
}
```

Sorting the two names means Alice → Bob and Bob → Alice both produce `dm:alice:bob`.

### 5.2 Sending: seq under the lock, dedupe, push

```java
Message send(String convId, String sender, String clientMsgId, String text) {
    Conversation c = conv(convId);
    Message m;
    c.lock.lock();
    try {
        if (!c.members.containsKey(sender)) throw new ChatException(sender + " is not in " + convId);
        Message dup = c.byClientId.get(sender + "/" + clientMsgId);
        if (dup != null) return dup;                         // retry after a lost ack: same message, same seq
        m = new Message("M" + ids.incrementAndGet(), convId, c.lastSeq() + 1, sender, clientMsgId, text, clock.getAsLong(), false);
        c.log.add(m);
        c.byClientId.put(sender + "/" + clientMsgId, m);
        c.members.get(sender).deliveredUpTo.set(m.seq());   // the sender obviously has it
        c.members.get(sender).readUpTo.set(m.seq());
        recipients = new ArrayList<>(c.members.keySet());
        for (String r : recipients) if (!r.equals(sender)) { DeviceConnection d = online.get(r); if (d != null) d.push(m); }
    } finally { c.lock.unlock(); }
    return m;
}
```

| Step | Simple meaning |
|---|---|
| the membership check | outsiders can't post |
| the dedupe lookup | the phone sends a **client message id** it generated; if the server has seen it from this sender, return the same message (same seq) |
| `lastSeq() + 1` | the next number in this conversation, assigned **under the lock** |
| sender cursors | the sender obviously has and has read their own message |
| push to online members | while holding the lock, so pushes leave in seq order |

**A production note (from the code):** doing network I/O while holding a lock is slow. Real servers hand messages to per-connection ordered queues instead, which keeps the order without holding the lock during I/O.

### 5.3 Sync: "everything after seq N"

```java
List<Message> sync(String convId, String userId, long afterSeq) {
    Conversation c = conv(convId);
    c.lock.lock();
    try {
        Member mem = c.members.get(userId);
        if (mem == null) throw new ChatException(userId + " is not a member");
        long from = Math.max(afterSeq, mem.joinedAtSeq);
        return List.copyOf(c.log.subList((int) Math.min(from, c.log.size()), c.log.size()));
    } finally { c.lock.unlock(); }
}
```

The log is a list where index = seq − 1, so "after seq N" is just `subList(N, size)`. `joinedAtSeq` means a new member never gets messages from before they joined. Removed members can't sync at all.

### 5.4 Receipts derived from cursors

```java
void ackDelivered(String convId, String userId, long upTo) { ... m.deliveredUpTo.accumulateAndGet(upTo, Math::max); }
void ackRead(String convId, String userId, long upTo) {
    ...
    m.readUpTo.accumulateAndGet(upTo, Math::max);
    m.deliveredUpTo.accumulateAndGet(upTo, Math::max);      // read implies delivered
}

Receipt receipt(String convId, long seq) {
    Conversation c = conv(convId);
    String sender = c.log.get((int) seq - 1).senderId();
    boolean allRead = true, allDelivered = true;
    for (Member m : c.members.values()) {
        if (m.userId.equals(sender) || m.joinedAtSeq >= seq) continue;
        allRead &= m.readUpTo.get() >= seq;
        allDelivered &= m.deliveredUpTo.get() >= seq;
    }
    return allRead ? Receipt.READ : allDelivered ? Receipt.DELIVERED : Receipt.SENT;
}
```

`accumulateAndGet(x, Math::max)` makes the cursors **only move forward**, even if an old ack arrives late. Members who joined after the message are ignored when computing its ticks.

### 5.5 Delete for everyone: a tombstone

```java
void deleteForEveryone(String convId, String user, long seq) {
    ...
    Message m = c.log.get((int) seq - 1);
    if (!m.senderId().equals(user)) throw new ChatException("Only the sender can delete");
    c.log.set((int) seq - 1, m.tombstone());
}
```

The message isn't removed (that would shift every later seq). Its slot is replaced by a **tombstone**: the same seq, empty text, `deleted = true`. Phones show "This message was deleted".

### 5.6 The client's hold-back buffer

```java
public synchronized void push(Message m) {
    if (!m.conversationId().equals(convId)) return;
    if (m.seq() <= applied || pending.containsKey(m.seq())) { duplicatesIgnored++; return; }
    pending.put(m.seq(), m);
    while (pending.containsKey(applied + 1)) timeline.add(pending.remove(++applied));   // drain in order
    server.ackDelivered(convId, userId, applied);
}

synchronized void syncFromServer() { for (Message m : server.sync(convId, userId, applied)) push(m); }
synchronized boolean hasGap() { return !pending.isEmpty(); }
```

**The analogy:** pages of a book arriving by post in random order. You put each page on a pile, and only add pages to the book when the **next** page number is in the pile. If page 25 never arrives, pages 26–50 wait in the pile. That's a **gap**, and the fix is to ask the server again ("send me everything after page 24").

| Line | Simple meaning |
|---|---|
| already applied or already pending | a duplicate: ignore it |
| `pending.put` | hold it back |
| the `while` | move every consecutive message into the timeline |
| `ackDelivered(applied)` | tell the server "I have everything up to here" |

---

## 6. Dry run (from the real output)

### Direct chat
| Step | Result |
|---|---|
| `direct(alice, bob)` and `direct(bob, alice)` | the same id `dm:alice:bob` |
| Alice sends "hi" (c-1), "are you free?" (c-2) | seq 1, 2; Bob's phone gets both |
| Alice's app re-sends c-1 (a lost ack) | **the same message M1**; Bob still sees it once |
| ticks for seq 2 | Bob's phone acked delivery → DELIVERED; Bob reads → READ |

### Offline and back
| Step | Result |
|---|---|
| Bob disconnects; Alice sends "call me", "urgent" | seq 3, 4: SENT only; Bob has 2 unread |
| Bob reconnects and syncs after seq 2 | gets seq 3, 4 → timeline: hi, are you free?, call me, urgent; now DELIVERED |

### Groups
| Step | Result |
|---|---|
| group "trip": alice (admin), bob, carol; two messages | Carol got both |
| only Bob reads | DELIVERED (Carol hasn't read) |
| Carol reads | READ |
| Dave (not a member) posts | refused |
| Bob (not an admin) adds Dave | refused |
| Alice adds Dave | Dave sees **0** old messages; after he posts, 1 |
| Alice removes Carol | Carol can't sync |
| Bob deletes his seq 2 | the timeline shows `goa?, <deleted>, hey all` |

### Out-of-order arrival
50 messages, shuffled, with 10 duplicates added and seq 25 **lost**:
- the device applies 1–24 and **stops at the gap**;
- `hasGap()` is true;
- `syncFromServer()` fetches everything after 24 → all 50 in order;
- the duplicates were ignored.

### Concurrency
16 members each send 200 messages to one group at once → 3,200 messages with seq 1..3200 and **no gaps**, the online watcher applied all 3,200 in order, and each sender's own messages kept their order.

---

## 7. Concurrency, explained simply

**The danger:** two senders read "last seq = 41" at the same moment and both create seq 42.

**The protection:** "read the last seq, create the message, append it" happens under the **conversation's lock**. One conversation's messages get one strict order; different conversations never wait for each other.

**On the client**, pushes and syncs can arrive on different threads, so `ClientDevice` methods are `synchronized`, and the hold-back buffer turns any arrival order into seq order.

---

## 8. Edge cases and common mistakes

**Edge cases handled:** retried sends, duplicate pushes, out-of-order pushes, a lost message (gap), offline users, new members (no old history), removed members, non-admins managing members, outsiders posting, deleting someone else's message.

**Common mistakes:**
1. Ordering by the phone's timestamp.
2. A global sequence for all conversations (a bottleneck), instead of one per conversation.
3. No client message id (retries create duplicates).
4. A delivered/read flag per message per member (huge storage) instead of cursors.
5. Removing deleted messages from the log (later seqs shift).
6. Applying pushes in arrival order on the device.

---

## 9. Interview questions and answers

**Q1. How do you keep messages in order?**
> The server assigns a sequence number per conversation, under that conversation's lock. Clients display by seq and use a hold-back buffer so out-of-order arrivals still show in order.

**Q2. How do you avoid duplicate messages when the app retries?**
> The phone generates a client message id per message. The server keeps (sender, clientMsgId) → message; a retry returns the stored message with its original seq.

**Q3. How does an offline user catch up?**
> Each device knows the last seq it applied. On reconnect it asks for everything after that. Cursor-based sync means the server never tracks individual messages per user.

**Q4. How do you compute ticks in a group?**
> From each member's delivered and read cursors: a message is READ when every other member's read cursor is at or past its seq, DELIVERED when every delivered cursor is, otherwise SENT.

**Q5. How does "delete for everyone" work?**
> The message's slot is replaced by a tombstone with the same seq, so the order doesn't shift; clients render "deleted".

**Q6. What does the client do when there's a gap?**
> It holds back later messages and requests a sync from its last applied seq, which fills the gap.

### Follow-up questions

**F1. "Scale to millions of users."**
> Connections are held by many gateway servers (WebSockets); a presence service maps users to gateways. Messages are stored in a database partitioned by conversation (Cassandra-style), and seqs come from the conversation's partition. Fan-out goes through a message broker.

**F2. "Very large groups (100,000 members)."**
> Don't push to everyone synchronously; fan out asynchronously in batches, or let large-group members pull on open. Receipts are often simplified or disabled for huge groups.

**F3. "Multiple devices per user."**
> Track cursors per device for delivery, and per user for read status; push to every online device.

**F4. "End-to-end encryption."**
> The server stores and routes encrypted blobs it can't read; seqs and cursors still work because they don't depend on the content.

**F5. "Typing indicators and online status."**
> Ephemeral events sent only to online members, not stored in the log.

---

## 10. Running the file

```bash
java ChatSystem.java      # 26/26 tests passed (including 16 concurrent senders producing seq 1..3200 with no gaps)
```

**One-line summary:** *"The server stamps a sequence number per conversation under that conversation's lock, de-duplicates retries by client message id, tracks only two cursors (delivered, read) per member for sync, unread counts and ticks, keeps deleted messages as tombstones, and clients use a hold-back buffer to apply messages strictly in seq order and sync when they see a gap."*
