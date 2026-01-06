# Security Policy

Since ResponseDiff is executed either manually or by a CI/CD pipeline and it interacts only with the configured REST endpoints, 
the propability that any risk is originating from it is pretty much zero.

## Supported Versions

Since ResponseDiff provides full backward compatibility it is always save to use the latest release.
Therefore the latest release is supported (only) - which is by now writing version 1.3.0 .

| Version | Supported          |
| ------- | ------------------ |
| 1.3.0   | :white_check_mark: |
| 1.2.0   | :x:                |
| 1.1.0   | :x:                |
| 1.0.x   | :x:                |

## Avoiding vulnerabilities

Every 2 - 4 week I check all project dependencies for updates and apply any update found as long as it does not introduce vulnerabilities by itself.
Special care is being taken if an entry exists in the file "owasp_cve_security_suppress.xml".
If an update introduces a new vulnerability it is only applied if it comes with a CVE score less than a previous one.

## Reporting a Vulnerability

Before reporting a vulnerability, please check if it is not already fixed in the current SNAPSHOT version.
If it is, feel free to use the SNAPSHOT version. Full backward compatibility is taken very seriously in this project.

In either cases, please visit the "[Discussions](https://github.com/kreutzr/responsediff/discussions)" tab, select the "[General / Report a vulnerability](https://github.com/kreutzr/responsediff/discussions/1)" discussion and just add a comment, 
describing the vulnerability as good as you can. If you have a CVE number, please provide it, too.

Thank you
