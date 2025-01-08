<p align="center"><a href="https://dataease.io"><img src="https://dataease.oss-cn-hangzhou.aliyuncs.com/img/dataease-logo.png" alt="DataEase" width="300" /></a></p>
<h3 align="center">Un outil BI open source simple et facile à utiliser</h3>
<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.html"><img src="https://img.shields.io/github/license/dataease/dataease?color=%231890FF" alt="License: GPL v3"></a>
  <a href="https://github.com/dataease/dataease"><img src="https://img.shields.io/github/stars/dataease/dataease?color=%231890FF&style=flat-square" alt="GitHub Stars"></a>
</p>

------------------------------

## Qu'est-ce que DataEase?

DataEase est un outil BI open-source conçu pour aider les utilisateurs à analyser rapidement les données et à obtenir des informations commerciales, leur permettant d'améliorer et d'optimiser leurs opérations. Il prend en charge une large gamme de sources de données, permettant aux utilisateurs de créer des graphiques avec une interface simple de glisser-déposer et de les partager sans effort.

**Les avantages de DataEase:**

-   Open Source : Aucune barrière, acquisition et installation en ligne rapides, mises à jour mensuelles.
-   Facile à utiliser : Simple d'utilisation ; l'analyse peut être effectuée en cliquant simplement avec la souris et en effectuant des actions de glisser-déposer.
-   Polyvalent : Prend en charge l'installation sur plusieurs plates-formes et propose diverses options d'intégration.
-   Partage sécurisé : Offre divers moyens de partage de données tout en garantissant la sécurité des données.

**Sources de données prises en charge:**

-   Bases de données OLTP : MySQL, Oracle, SQL Server, PostgreSQL, MariaDB, Db2, TiDB, MongoDB-BI, etc.
-   Bases de données OLAP : ClickHouse, Apache Doris, Apache Impala, StarRocks, etc.
-   Entrepôts de données/Lacs de données : Amazon RedShift, etc.
-   Fichiers de données : Excel, CSV, etc.
-   Sources de données API.

## Démarrage rapide

```
# Préparer un serveur Linux avec au moins 2 processeurs et 4 Go de mémoire vive, puis exécuter le script d'installation automatique (un clic) suivant en tant qu'utilisateur root:

curl -sSL https://dataease.oss-cn-hangzhou.aliyuncs.com/quick_start_v2.sh | bash

# Username: admin
# Password: DataEase@123456
```

## Pile technologique

-   Interface utilisateur: [Vue.js](https://vuejs.org/), [Element](https://element.eleme.cn/)
-   Bibliothèque de visualisation: [AntV](https://antv.vision/zh)
-   Côté serveur: [Spring Boot](https://spring.io/projects/spring-boot)
-   Base de données: [MySQL](https://www.mysql.com/)
-   Traitement des données: [Apache Calcite](https://github.com/apache/calcite/), [Apache SeaTunnel](https://github.com/apache/seatunnel)
-   Infrastructure: [Docker](https://www.docker.com/)

## Sécurité

Si vous découvrez des problèmes de sécurité, veuillez nous contacter via: wei@fit2cloud.com.

## Licence

Copyright (c) 2014-2024 [FIT2CLOUD](https://fit2cloud.com/), Tous droits réservés.

Sous licence The GNU General Public License version 3 (GPLv3) (la « Licence ») ; vous n'êtes pas autorisé à utiliser ce fichier sauf en conformité avec la Licence. Vous pouvez obtenir une copie de la Licence à l'adresse suivante

<https://www.gnu.org/licenses/gpl-3.0.html>

Sauf obligation imposée par la loi applicable ou accord écrit, le logiciel distribué sous la Licence est distribué « tel quel », sans garantie ni condition de quelque nature que ce soit, qu'elle soit expresse ou implicite. Consultez la Licence pour le langage spécifique régissant les autorisations et les limitations prévues par la Licence.
