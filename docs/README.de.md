<p align="center"><a href="https://dataease.io"><img src="https://dataease.oss-cn-hangzhou.aliyuncs.com/img/dataease-logo.png" alt="DataEase" width="300" /></a></p>
<h3 align="center">Ein einfaches und benutzerfreundliches Open-Source-BI-Tool</h3>
<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.html"><img src="https://img.shields.io/github/license/dataease/dataease?color=%231890FF" alt="License: GPL v3"></a>
  <a href="https://github.com/dataease/dataease"><img src="https://img.shields.io/github/stars/dataease/dataease?color=%231890FF&style=flat-square" alt="GitHub Stars"></a>
</p>

------------------------------
## Was ist DataEase?

DataEase ist ein Open-Source-BI-Werkzeug (Business Intelligence-Werkzeug), das dazu ausgelegt ist, Benutzern zu helfen, Daten schnell zu analysieren und Geschäftsinformationen zu gewinnen, sodass sie ihre Geschäftsvorgänge verbessern und optimieren können. Es unterstützt eine Vielzahl von Datenquellen und ermöglicht es Benutzern, mit einer einfachen Drag-and-Drop-Schnittstelle Diagramme zu erstellen und diese mühelos zu teilen. 

**Vorteile von DataEase:**

-   Open Source: Keine Hindernisse, schnelle Online-Erwerb und -Installation, monatliche Updates.
-   Benutzerfreundlich: Einfach zu bedienen; die Analyse kann mit einfachen Mausklicks und Drag-and-Drop-Aktionen durchgeführt werden.
-   Vielseitig: Unterstützt die Installation auf mehreren Plattformen und vielfältige Einbettungsoptionen.
-   Sicherer Datenaustausch: Bietet verschiedene Methoden zum Datenaustausch und gewährleistet dabei die Datensicherheit.

**Unterstützte Datenquellen:**

-   OLTP-Datenbanken: MySQL, Oracle, SQL Server, PostgreSQL, MariaDB, Db2, TiDB, MongoDB-BI usw.
-   OLAP-Datenbanken: ClickHouse, Apache Doris, Apache Impala, StarRocks usw.
-   Datenlager / Data Warehouses / Datenseen: Amazon RedShift usw.
-   Datendateien: Excel, CSV usw.
-   API-Datenquellen.

## Schnellstart

```
# Bereiten Sie einen Linux-Server mit mindestens 2 CPUs und 4 GB RAM vor und führen Sie dann das folgende Skript für die Ein-Klick-Installation als Root-Benutzer aus:

curl -sSL https://dataease.oss-cn-hangzhou.aliyuncs.com/quick_start_v2.sh | bash

# Benutzername: admin
# Passwort: DataEase@123456
```

## Technologiestack

-   Frontend: [Vue.js](https://vuejs.org/), [Element](https://element.eleme.cn/)
-   Visualisierungsbibliothek: [AntV](https://antv.vision/zh)
-   Backend: [Spring Boot](https://spring.io/projects/spring-boot)
-   Datenbank: [MySQL](https://www.mysql.com/)
-   Datenverarbeitung: [Apache Calcite](https://github.com/apache/calcite/), [Apache SeaTunnel](https://github.com/apache/seatunnel)
-   Infrastruktur: [Docker](https://www.docker.com/)

## Sicherheit

Wenn Sie irgendwelche Sicherheitsbedenken entdecken, kontaktieren Sie uns bitte über: wei@fit2cloud.com.

## Lizenz

Copyright (c) 2014-2024 [FIT2CLOUD](https://fit2cloud.com/), Alle Rechte vorbehalten.

Unterliegt der GNU General Public License Version 3 (GPLv3) (die „Lizenz“); Sie dürfen diese Datei nicht verwenden, es sei denn, Sie stimmen der Lizenz zu. Sie können eine Kopie der Lizenz unter folgender Adresse einsehen:

https://www.gnu.org/licenses/gpl-3.0.html

Sofern nicht durch anwendbares Recht vorgeschrieben oder schriftlich vereinbart, wird Software, die unter dieser Lizenz verteilt wird, auf der Grundlage „AS IS“ verteilt, ohne jegliche Zusicherungen oder Bedingungen, weder ausdrücklich noch stillschweigend. Lesen Sie die Lizenz für die spezifischen Bestimmungen zur Gewährung von Berechtigungen und Einschränkungen unter dieser Lizenz.
