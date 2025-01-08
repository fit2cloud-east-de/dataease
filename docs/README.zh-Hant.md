<p align="center"><a href="https://dataease.io"><img src="https://dataease.oss-cn-hangzhou.aliyuncs.com/img/dataease-logo.png" alt="DataEase" width="300" /></a></p>
<h3 align="center">簡單易用的開源 BI 工具</h3>
<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.html"><img src="https://img.shields.io/github/license/dataease/dataease?color=%231890FF" alt="License: GPL v3"></a>
  <a href="https://github.com/dataease/dataease"><img src="https://img.shields.io/github/stars/dataease/dataease?color=%231890FF&style=flat-square" alt="GitHub Stars"></a>
</p>

------------------------------

## 什麼是 DataEase？

DataEase 是開源的 BI 工具，幫助用戶快速分析數據並洞察業務趨勢，從而實現業務的改進與優化。DataEase 支持豐富的數據源連接，能夠通過拖拉拽方式快速製作圖表，並可以方便地與他人分享。

**DataEase 的優勢：**

-   開源開放：零門檻，線上快速獲取和安裝，按月迭代；
-   簡單易用：極易上手，通過鼠標點擊和拖拉即可完成分析；
-   全場景支持：多平台安裝和多樣化嵌入支持；
-   安全分享：支持多種數據分享方式，確保數據安全。

**DataEase 支持的數據源：**

-   OLTP 數據庫： MySQL、Oracle、SQL Server、PostgreSQL、MariaDB、Db2、TiDB、MongoDB-BI 等；
-   OLAP 數據庫： ClickHouse、Apache Doris、Apache Impala、StarRocks 等；
-   數據倉庫/數據湖： Amazon RedShift 等；
-   數據文件： Excel、CSV 等；
-   API 數據源。

如果您需要向團隊介紹 DataEase，可以使用這個 [官方 PPT 材料](https://fit2cloud.com/dataease/download/introduce-dataease_202411.pdf)。

## 快速開始

**桌面版：**

您可以在 PC 上安裝 DataEase 桌面版，下載地址為：https://dataease.cn/

**伺服器版：**

```
# 準備一台 2 核 4G 以上的 Linux 伺服器，並以 root 用戶運行以下一鍵安裝腳本：
```

curl -sSL https://dataease.oss-cn-hangzhou.aliyuncs.com/quick_start_v2.sh | bash


# 使用者名稱: admin
# 密碼: DataEase@123456
```

你也可以透過 [1Panel 應用商店](https://dataease.io/docs/v2/installation/1panel_installation/) 快速部署 DataEase。如果是用於生產環境，建議使用 [離線安裝包方式](https://dataease.io/docs/v2/installation/offline_INSTL_and_UPG/) 進行安裝部署。

如你有更多問題，可以查看線上文件，或者透過論壇與我們交流。

-   [線上文件](https://dataease.io/docs/)
-   [社區論壇](https://bbs.fit2cloud.com/c/de/6)

## UI 展示

<table style="border-collapse: collapse; border: 1px solid black;">
  <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "https://github.com/dataease/dataease/assets/41712985/8dbed4e1-39f0-4392-aa8c-d1fd83ba42eb" alt="DataEase 工作台"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "https://github.com/dataease/dataease/assets/41712985/7c54cb07-51ef-4bb6-a931-8a95c64c7e11" alt="DataEase 仪表板"   /></td>
  </tr>

  <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "https://github.com/dataease/dataease/assets/41712985/ffa79361-a7b3-4486-b14a-f3fd3a28f01a" alt="DataEase 数据源"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "https://github.com/dataease/dataease/assets/41712985/bb28f4e4-636e-4ab0-85c5-1dfbd7a5397e" alt="DataEase 模板中心"   /></td>
  </tr>
</table>

## 技術棧

-   前端：[Vue.js](https://vuejs.org/)、[Element](https://element.eleme.cn/)
-   圖庫：[AntV](https://antv.vision/zh)
-   後端：[Spring Boot](https://spring.io/projects/spring-boot)
-   資料庫：[MySQL](https://www.mysql.com/)
-   資料處理：[Apache Calcite](https://github.com/apache/calcite/)、[Apache SeaTunnel](https://github.com/apache/seatunnel)
-   基礎設施：[Docker](https://www.docker.com/)

## 飛致雲的其他明星專案

- [1Panel](https://github.com/1panel-dev/1panel/) - 現代化、開源的 Linux 伺服器運維管理面板
- [MaxKB](https://github.com/1panel-dev/MaxKB/) - 基於 LLM 大語言模型的開源知識庫問答系統
- [JumpServer](https://github.com/jumpserver/jumpserver/) - 廣受歡迎的開源堡壘機
- [Halo](https://github.com/halo-dev/halo/) - 強大易用的開源建站工具
- [MeterSphere](https://github.com/metersphere/metersphere/) - 新一代的開源持續測試工具

## License

Copyright (c) 2014-2024 [FIT2CLOUD 飞致云](https://fit2cloud.com/), All rights reserved.

Licensed under The GNU General Public License version 3 (GPLv3)  (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

<https://www.gnu.org/licenses/gpl-3.0.html>

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
