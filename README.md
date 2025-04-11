# responsediff

ResponseDiff is a Java based regression testing tool that compares the HTTP response of a reference installation with that of a candidate (software under test) installation.
Found differences may be marked as intended (and may be given a reference to a ticket) by the tester for later tests.
It also supports functional testing (e.g., expected values) and non-functional testing (e.g., maximum response times).

Responses are assumed to be JSON but since ResponseDiff supports response filters, the native response format may be converted to JSON at runtime.

Values received in one response may be read via JSONPath and then used as parameters for following requests. Parameters may also read from a file or database for mass data testing.

All tests performed are compiled into a report (Asciidoc and PDF by default). 
The entire raw data (variables, requests and respones) is made available for further inspection as XML.
