---

marp: true
theme: default
class: invert
paginate: true
--------------

<!-- Slide 1 -->

# Building Resilient and Scalable Client Extensions

Event-Driven Architectures for Custom Solutions on Liferay

---

## Client Extensions: Quick recap

* Introduced to keep DXP core untouched
* Each virtual instance can plug in custom logic
* Designed for upgrade-safe extensibility
* Essential for SaaS

---

## What We Observed in the Field

* Most Client Extensions remained synchronous
* Tight coupling to REST and blocking calls
* Difficult to handle failure gracefully
* Scalability was limited

---

## The Gaps We Had to Close

* No reference architecture for reactive design
* No messaging infrastructure by default
* No automation for queue provisioning

➡️ Without these, **resilience and scalability were hard to achieve**.

---

## What We Did in the Last 2 Weeks

✅ Added message broker to default stack
✅ Automatic queue provisioning for Object Definitions
✅ Built a sample reactive Client Extension
✅ Shared the architecture and design patterns

---

## What We’re Demonstrating Today

A working example of:

* Reactive, event-driven architecture
* Full decoupling between Liferay and extensions
* Failure handling without blocking flows
* Scalable and maintainable by design

---

# Let’s See It in Action

🎬 Live Demo: Client Extension with Event-Driven Architecture