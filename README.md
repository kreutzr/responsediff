<img src="doc/img/responsediff-logo_320x160.png" alt="ResponseDiff"/>

ResponseDiff is a Java based **regression testing** tool that compares the **HTTP** response of a reference installation with that of a candidate (software under test) installation.
Found differences may be marked as intended (and may be given a **reference to a ticket** for documentation) by the tester for later tests.
It also supports **functional testing** (e.g., expected values) and **non-functional** testing (e.g., maximum response times).

Responses are assumed to be returned as **JSON** but since ResponseDiff supports individual **response filters**, the native response format may be converted beforehand into JSON at runtime.

Values received in a response may be read via **JSONPath** and then used as **parameters for following requests** which allows flexible test flows. Parameters may also be read from a file or database for **mass data testing**.

The results of the performed tests are compiled together with **statistics** and their **curl** commands into a **report** (Asciidoc and PDF by default).
The entire raw data (variables, requests and respones) is made available for **further inspection as XML**.

**Manuals** are currently provided in [English](doc/manual_en.adoc) and [German](doc/manual_de.adoc).

The **release-notes** can be found [here](doc/release-notes.adoc).

A simple **getting started project** is provided [here](https://github.com/kreutzr/responsediff-demo). The start script `start-responsediff` from the `doc` folder can be used for an immediate start.
