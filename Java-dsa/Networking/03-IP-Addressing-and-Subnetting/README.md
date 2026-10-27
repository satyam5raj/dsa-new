# Networking 03 · IP addressing and subnetting

> Every packet carries a source and a destination IP. Knowing how addresses, masks, gateways, NAT,
> DHCP and ARP fit together lets you design VPCs, debug "can't reach X", and answer subnetting
> questions instantly.

Run it: `java Subnetting.java` → **40/40 checks**: a CIDR calculator, address classification, VPC
planning and overlap detection, the host routing decision, a NAT/PAT table, DHCP/ARP and IPv6
compression.

---

## 1. IPv4 addresses

An IPv4 address is **32 bits**, written as 4 decimal bytes: `192.168.1.77` =
`11000000.10101000.00000001.01001101`. That's 2³² ≈ **4.3 billion** addresses, which ran out years
ago. Hence NAT (§6) and IPv6 (§8).

Every address has two parts:

```
192.168.1.77/26
└─ network part (first 26 bits) ─┘└ host part (last 6 bits) ┘
```

- **Subnet mask:** 1s for the network part. `/26` = `255.255.255.192`.
- **CIDR notation** (`/26`) replaced the old classes A/B/C. It allows any prefix length.

## 2. Subnetting: the calculations you must do quickly

For prefix `/n`:

| Quantity | Formula | `192.168.1.0/24` | `192.168.1.77/26` |
|---|---|---|---|
| addresses | `2^(32−n)` | 256 | **64** |
| usable hosts | `2^(32−n) − 2` (minus network and broadcast) | 254 | **62** |
| network address | `ip AND mask` (host bits = 0) | 192.168.1.0 | **192.168.1.64** |
| broadcast address | host bits = 1 | 192.168.1.255 | **192.168.1.127** |
| host range | network+1 … broadcast−1 | .1 – .254 | **.65 – .126** |
| netmask | | 255.255.255.0 | 255.255.255.192 |

**How to find the block quickly:** a /26 has a block size of 64, so its networks start at 0, 64,
128 and 192. 77 falls in the 64 block, so the network is .64 and the broadcast is 64 + 64 − 1 =
.127.

| Prefix | Block size (last octet) | Mask | Usable |
|---|---|---|---|
| /24 | 256 | 255.255.255.0 | 254 |
| /25 | 128 | .128 | 126 |
| /26 | 64 | .192 | 62 |
| /27 | 32 | .224 | 30 |
| /28 | 16 | .240 | 14 |
| /29 | 8 | .248 | 6 |
| /30 | 4 | .252 | 2 (old point-to-point links) |
| /31 | 2 | .254 | **2**: point-to-point, no broadcast (RFC 3021) |
| /32 | 1 | .255 | a single host (routes, allowlists) |

Also: `/20` = `255.255.240.0` = 4,096 addresses, and `/16` = 65,536.

**192.168.1.0/24 vs 192.168.1.0/26:** the /24 is the whole 256-address block. The /26 is only the
first quarter (.0–.63), and a /24 splits into four /26s.

**Same subnet?** Each host ANDs both addresses with its mask and compares. `10.0.1.200` and
`10.0.1.5` are in the same /24 but **not** the same /25 (.0–.127 vs .128–.255).

## 3. Special address ranges

| Range | Meaning |
|---|---|
| `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16` | **private** (RFC 1918): not routed on the Internet. Note `172.16–172.31` only: **172.32.0.5 is public** |
| `100.64.0.0/10` | carrier-grade NAT (ISPs, some Kubernetes setups) |
| `127.0.0.0/8` | **loopback**: the whole /8, not just 127.0.0.1. Never leaves the host |
| `169.254.0.0/16` | **link-local**: self-assigned when DHCP fails; `169.254.169.254` is the cloud **metadata service** (a classic SSRF target) |
| `224.0.0.0/4` | multicast |
| `0.0.0.0` | "this host / any address" (`listen 0.0.0.0` = all interfaces) |
| `255.255.255.255` | limited broadcast |

**Public vs private:** public IPs are globally unique and routable; private IPs are reused in every
home and VPC and need **NAT** to reach the Internet.

**Static vs dynamic:** static addresses are configured by hand (servers, gateways, DNS) or are
reserved Elastic IPs. Dynamic addresses are leased by **DHCP** (laptops, ordinary cloud
instances). In the cloud, use DNS names or service discovery, not hard-coded IPs.

## 4. Designing VPC address space (a senior question)

The program splits `10.0.0.0/16` into sixteen `/20`s (4,096 addresses each) and assigns one per
tier and availability zone:

| Tier | az-a | az-b | az-c |
|---|---|---|---|
| public (load balancers, NAT gateways) | 10.0.0.0/20 | 10.0.16.0/20 | 10.0.32.0/20 |
| private-app (services, Kubernetes nodes and pods) | 10.0.48.0/20 | 10.0.64.0/20 | 10.0.80.0/20 |
| private-db | 10.0.96.0/20 | 10.0.112.0/20 | **10.0.128.0/20** |

Seven /20s are left for growth.

**Rules of thumb:**

1. **Never overlap** with anything you might connect: other VPCs (peering, Transit Gateway), the
   office or VPN, partner networks, Docker or Kubernetes internal ranges. The program flags
   `10.0.0.0/16` vs `10.0.64.0/18` (office) and Docker's `172.17.0.0/16` inside `172.16.0.0/12`.
   Overlaps make routing ambiguous, and fixing them later means re-IPing everything.
2. **Plan big.** Kubernetes with VPC-native pod networking eats IPs fast (every pod gets one), and
   resizing a VPC is painful.
3. Account for **provider reservations**: AWS reserves 5 addresses per subnet, so a /20 has 4,091
   usable.
4. Split by **tier** (security-group and routing boundaries) and by **AZ** (fault isolation).
5. Keep a central **IPAM** registry for all teams.

## 5. The default gateway

When a host sends a packet it decides:

```
if destination is in my subnet: deliver directly (ARP for the destination's MAC)
else:                          send to the default gateway (ARP for the gateway's MAC)
```

**Incorrect default gateway** (a senior question): same-subnet traffic **still works**, but
everything else (other subnets, the Internet, DNS servers elsewhere) fails. Symptoms: "I can ping
the box next to me but not google.com". A **wrong prefix** is sneakier: with /16 instead of /24,
`10.0.2.7` looks local, the host ARPs for it directly, gets no answer, and the traffic fails even
though routing would have worked.

**Loopback** (`127.0.0.1`, `::1`): traffic never leaves the host. A service bound to `127.0.0.1`
is unreachable from other machines, a classic Docker gotcha: bind to `0.0.0.0` inside containers.

## 6. NAT and PAT

**NAT** rewrites addresses as packets cross a boundary. **PAT** (port address translation, also
called NAPT or "masquerade") lets **many private hosts share one public IP** by also rewriting
**ports**:

| Private side | Public side (what the Internet sees) |
|---|---|
| 192.168.1.10:51000 → 93.184.216.34:443 | **203.0.113.7:40000** → 93.184.216.34:443 |
| 192.168.1.11:51000 → 93.184.216.34:443 | **203.0.113.7:40001** → 93.184.216.34:443 |

- Two hosts used the **same** private port; the NAT keeps them apart with different public ports.
- Replies to 203.0.113.7:**40001** map back to **192.168.1.11:51000**.
- **Unsolicited inbound** traffic has no mapping, so it's dropped. That's why servers behind NAT
  need port forwarding or a load balancer, and why P2P apps need NAT traversal (STUN/TURN).

**Production gotchas:**

- **SNAT port exhaustion:** one public IP can hold only ~64k simultaneous connections to the
  same destination IP and port. Heavy egress through one cloud NAT gateway to one API hits that
  limit; you see connection timeouts. Fixes: more NAT IPs, connection pooling, private endpoints.
- NAT tables have **idle timeouts**. A long-idle TCP connection gets silently dropped, which is why
  keepalives matter (chapter 04).

## 7. DHCP, ARP and Neighbor Discovery

**DHCP (DORA):** a new host has no IP yet.

1. **Discover:** a broadcast: "any DHCP server?"
2. **Offer:** "take 10.0.1.57/24, gateway 10.0.1.1, DNS 10.0.0.2, lease 1 h".
3. **Request:** "I accept that".
4. **Ack:** confirmed. The host renews at 50 % of the lease.

If nobody answers, the host self-assigns `169.254.x.x`. Seeing that address means "DHCP is broken".

**ARP** (IPv4): before sending a frame, the host needs the next hop's MAC. It broadcasts "**who
has 10.0.1.1? tell 10.0.1.5**", the owner replies, and the result is **cached**. In the program,
4 sends needed only 2 broadcasts. Stale ARP entries after an IP moves to a new machine (failover)
cause brief outages, so failover systems send **gratuitous ARP**.

**IPv6 Neighbor Discovery** replaces ARP with ICMPv6 multicast (neighbour solicitation and
advertisement), and adds router discovery and SLAAC (address self-configuration).

**DNS** maps names to IPs, sitting on top of all this (chapter 05).

## 8. IPv6

- **128-bit** addresses in 8 hex groups. `2001:0db8:0000:0000:0000:8a2e:0370:7334` compresses to
  **`2001:db8::8a2e:370:7334`**: drop the leading zeros in each group and replace the longest run of
  zero groups with `::` (only once).
- Loopback `::1`; **link-local `fe80::/10`** (every interface, used by Neighbor Discovery); the
  global unicast range is `2000::/3`.
- A standard subnet is **/64** (2⁶⁴ hosts), so there's no host-count subnet maths. You plan
  /48 → /56 → /64.
- There's no broadcast (multicast instead), no NAT needed, and routers never fragment.

| | IPv4 | IPv6 |
|---|---|---|
| size | 32 bits | 128 bits |
| shortage | yes, so NAT everywhere | no |
| config | DHCP / static | SLAAC and/or DHCPv6 |
| neighbour resolution | ARP (broadcast) | NDP (multicast ICMPv6) |
| header checksum | yes | no |

**Why is a service reachable over IPv4 but not IPv6?** (a senior question) Walk the stack; the
program's checklist finds the first failure:

1. Is there an **AAAA** record (and is it correct)?
2. Does the server **listen on `[::]`**, not just `0.0.0.0`? This is the most common cause.
3. Do security groups, firewalls and NACLs allow IPv6? (They're separate rules from IPv4.)
4. Is there a route to `::/0` via an IPv6 gateway, and an IPv6 address on the load balancer?
5. PMTUD: ICMPv6 must not be blocked. IPv6 relies on it because routers can't fragment.

Clients with **Happy Eyeballs** try both protocols and fall back, which can hide the problem as
"sometimes slow".

---

## Senior-level questions

**1. What happens when a server has an incorrect default gateway?** It can still talk to its own
subnet (ARP works) but can't reach anything off-subnet: the Internet, other VPC subnets, maybe its
DNS server. Responses to clients in other networks never make it back. Check `ip route`.

**2. How does NAT allow multiple private hosts to share a public IPv4 address?** PAT rewrites the
source IP **and port** on outbound packets and records the mapping in a translation table. Replies
are matched by public port and translated back. Inbound connections without a mapping are dropped.
The limits are ~64k ports per destination and idle-timeout expiry.

**3. How would you design IP ranges for services across multiple VPC subnets?** Pick a
non-overlapping block per VPC from a central IPAM plan (e.g. `10.0.0.0/16` per environment/region),
split by tier and AZ (§4), leave room to grow, account for Kubernetes pod IPs and provider
reservations, and avoid Docker/VPN/office ranges.

**4. Why might a service be reachable through IPv4 but not IPv6?** A missing or wrong AAAA record,
the app listening only on `0.0.0.0`, IPv6 firewall or security-group rules missing, no IPv6 route or
load-balancer listener, or ICMPv6 blocked (which breaks PMTUD and neighbour discovery).

---

**Next:** [04 · TCP and UDP](../04-TCP-and-UDP/README.md)
