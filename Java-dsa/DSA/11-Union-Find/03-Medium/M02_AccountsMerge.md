# M02 · Accounts Merge

| | |
|---|---|
| **LeetCode** | [#721 Accounts Merge](https://leetcode.com/problems/accounts-merge/) |
| **Difficulty** | Medium |
| **Pattern** | Union-Find over **emails** (strings); union all the emails of one account |
| **Code** | [M02_AccountsMerge.java](M02_AccountsMerge.java) |
| **Run** | `java M02_AccountsMerge.java` |

Union-Find basics are explained in [E01](../02-Easy/E01_FindIfPathExistsUnionFind.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

`accounts[i] = [name, email1, email2, …]`. Two accounts belong to the same person if they share **any** email (transitively). Merge them, and output each person as `[name, sorted emails…]`.

```
[["John","a@x","b@x"], ["John","b@x","c@x"], ["Mary","m@x"], ["John","j@x"]]
→ [["John","a@x","b@x","c@x"], ["Mary","m@x"], ["John","j@x"]]
```

There are two different Johns: **the same name does not mean the same person**.

---

## 2. How to think about it

### What are the elements? The emails
The accounts only say "these emails belong to one person":

```
for each account:  union(its first email, every other email of that account)
account 1 links a–b,  account 2 links b–c   →   a, b, c end up in one set (via b)
```

### Steps
1. map email → an int id (and remember the owner's name)
2. union the emails inside each account
3. group the emails by `find(id)`; sort each group; prepend the name

### The graph view (also fine)
Emails are vertices, each account adds edges, and a DFS lists each component. Union-Find is shorter and has no recursion-depth issues.

---

## 3. Java concepts used in this problem

### 3.1 Turning strings into Union-Find ids

```java
if (id.putIfAbsent(acc.get(i), id.size()) == null) owner.put(acc.get(i), acc.get(0));
```

- `id.size()` is the next unused id (0, 1, 2, …).
- `putIfAbsent` returns **null** only if the email was new, so the owner's name is recorded once per email.

Union-Find wants ints 0..n−1; a `HashMap<String, Integer>` translates. (The alternative is a Union-Find keyed by strings, with `Map<String, String> parent`.)

### 3.2 `TreeSet` for the sorted output

```java
groups.computeIfAbsent(find(id.get(email)), k -> new TreeSet<>()).add(email);
```

Each group is a `TreeSet`, which keeps the emails sorted automatically. `g.first()` is any email of the group, used to look up the owner's name.

### 3.3 `Collections.disjoint` (in the referee)

`!Collections.disjoint(a, b)` is true when the two sets share at least one element.

---

## 4. Approach 1: Repeatedly merge any two overlapping accounts (the referee)

### Code ([M02_AccountsMerge.java:76-96](M02_AccountsMerge.java#L76-L96))

```java
static List<List<String>> brute(List<List<String>> accounts) {
    List<String> names = new ArrayList<>();
    List<Set<String>> sets = new ArrayList<>();
    for (List<String> a : accounts) { names.add(a.get(0)); sets.add(new TreeSet<>(a.subList(1, a.size()))); }
    boolean merged = true;
    while (merged) {
        merged = false;
        outer:
        for (int i = 0; i < sets.size(); i++) for (int j = i + 1; j < sets.size(); j++) {
            if (!Collections.disjoint(sets.get(i), sets.get(j))) {
                sets.get(i).addAll(sets.remove(j));
                names.remove(j);
                merged = true;
                break outer;
            }
        }
    }
    List<List<String>> out = new ArrayList<>();
    for (int i = 0; i < sets.size(); i++) { List<String> row = new ArrayList<>(List.of(names.get(i))); row.addAll(sets.get(i)); out.add(row); }
    return out;
}
```

| Line | What it does |
|---|---|
| `sets`, `names` | one email set and name per account |
| find any overlapping pair | merge it, then restart (`break outer`) |
| until no merge happens | every remaining set is a separate person |

Many passes of O(n²) pairs: slow, but clearly correct.

---

## 5. Approach 2: Union-Find on email ids (the file's main solution)

### Code ([M02_AccountsMerge.java:37-66](M02_AccountsMerge.java#L37-L66))

```java
static int[] parent;

static int find(int x) {
    while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
    return x;
}

static List<List<String>> accountsMerge(List<List<String>> accounts) {
    Map<String, Integer> id = new HashMap<>();
    Map<String, String> owner = new HashMap<>();
    for (List<String> acc : accounts)
        for (int i = 1; i < acc.size(); i++)
            if (id.putIfAbsent(acc.get(i), id.size()) == null) owner.put(acc.get(i), acc.get(0));
    parent = new int[id.size()];
    for (int i = 0; i < parent.length; i++) parent[i] = i;
    for (List<String> acc : accounts) {
        int first = id.get(acc.get(1));
        for (int i = 2; i < acc.size(); i++) parent[find(id.get(acc.get(i)))] = find(first);   // same person
    }
    Map<Integer, TreeSet<String>> groups = new HashMap<>();
    for (String email : id.keySet()) groups.computeIfAbsent(find(id.get(email)), k -> new TreeSet<>()).add(email);
    List<List<String>> result = new ArrayList<>();
    for (TreeSet<String> g : groups.values()) {
        List<String> row = new ArrayList<>();
        row.add(owner.get(g.first()));
        row.addAll(g);
        result.add(row);
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| the id loop | every distinct email gets an id; its first owner's name is remembered |
| the union loop | link every email of an account to that account's first email |
| `parent[find(x)] = find(first)` | union by linking the roots |
| the grouping loop | emails with the same root belong to one person |
| `owner.get(g.first())` | the person's name (all of a person's emails have the same owner) |

### Dry run: the example

ids: a@x 0, b@x 1, c@x 2, m@x 3, j@x 4

| account | unions | groups |
|---|---|---|
| John a, b | b → a | {a, b} |
| John b, c | c → (root of b) | {a, b, c} |
| Mary m | (only one email) | {m} |
| John j | | {j} |

→ `[John, a@x, b@x, c@x]`, `[Mary, m@x]`, `[John, j@x]` ✓

### Complexity
- **Time O(E log E)** (sorting the emails dominates; the unions are about O(1)).
- **Space O(E)** (E = the total number of emails).

---

## 6. Comparing the approaches

| | Repeated merging | Union-Find |
|---|---|---|
| Time | O(n² · rounds · L) | **O(E log E)** |
| Code | short but slow | short and fast |

The file checks Union-Find against repeated merging on 500 random account lists.

---

## 7. Edge cases and common mistakes

**Edge cases:** an account with one email, the same email repeated inside an account, two people with the same name, chains of accounts linked only through intermediates.

**Mistakes:**
1. Merging by name (different people can share a name).
2. Only merging accounts that **directly** share an email (it must be transitive).
3. Forgetting to sort the emails in each group.

---

## 8. Interview questions and answers

**Q1. Why union emails rather than accounts?**
> Emails are what accounts share. Unioning each account's emails with each other makes any shared email automatically connect the accounts, transitively.

**Q2. Could you use DFS instead?**
> Yes: build a graph with emails as vertices and edges within each account, then collect each component. Same complexity; Union-Find avoids recursion.

### Follow-up questions

**F1. Return the indices of the accounts that were merged.**
> Union the accounts (index ids) through an email → first-account map.

**F2. Emails arrive in a stream.**
> Union-Find handles it incrementally; each new account only adds unions.

**F3. Sentence Similarity II (LC 737).**
> Union the similar word pairs; two sentences are similar if each pair of words has the same root.

---

## 9. Running the file

```bash
java M02_AccountsMerge.java      # 2/2 tests passed (including 500 random account lists vs repeated merging)
```

**One-line summary:** *"Give each email an id, union all the emails within each account, then group the emails by root, sort each group, and prepend the owner's name."*
