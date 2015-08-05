*---
template: howto.html.jinja2
title: Howto » Entry Rules
uid: addc60ac-1daf-11e5-9a21-1697f925ec7b
---*

Entry rules
===========

**Aggregator** uses user-defined entry rules to filter out the entries of a feed.

There are two types of rules: **exclude rules**, used to mark which entries should be hidden while aggregating a feed,
and **include rules**, used to cancel the effect of some exclude rules.

Every entry rule defines a **mask** used to match the entries for which the rule **action** is applied. The `%`
character is used to match zero, one or multiple characters, and can be used multiple times in a **mask**.

For example, the `%aggregator%` title mask will match every entry that contains `aggregator` in the title. While the `% 
Vol % Ch %` title mask will match every entry that contains both `Vol` and `Ch` (in the same order as present in the
mask) in the title.

Excluding unwished entries
--------------------------

The easiest way to define rules that hide entries of a feed is by using the **exclude** action:

1. Select the entry which you want to exclude by *long pressing* it.
2. Click the *exclude* action from the action bar.

*NOTE: Depending on your device type, the **exclude** action can be found either as as an action icon or as an option in
the overflow menu.*

*HINT: Long pressing an icon in the action bar reveals the action name.*

Excluding all entries except the whished ones
---------------------------------------------

This is achieved by defining at least two rules:

1. An **exclude** rule that hides every entry by using the following title mask: `%`
2. One or more **include** rules that will cancel the effect of the **exclude** rule.

These rules are preferably created from the *entry rules* activity, which can be opened by clicking the **entry rules**
option from the overflow menu.

*NOTE: The **entry rules** activity lists all the rules of a feed, including the global ones, while also showing how
many entries are matched by a rule.*