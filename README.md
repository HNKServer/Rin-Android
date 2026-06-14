# AndroidRin

A lightweight Android server application with a mobile client interface.

![AndroidEw](image.jpg)

## Overview

Basically just the original AndroidEw with ZH-CHT support and localhost CDN with Android pathway mapping function.

All for ZH language support and convenience.

基本上就是原版的 AndroidEw 加上了繁体中文语言包支持以及有 Android 路径映射功能的本地 CDN 数据包分发功能。

一切都是为了支持中文语言包和便捷。

## Usage

### Playing with EN/JP

Just follow the APP's internal instructions with default configuration. This modified version server is also compatible with the original versions of clients.

只需遵从 APP 内置的引导提示，并且保持默认设置即可。修改版服务端依旧兼容各个原始版本的客户端。

### Playing with ZH-CHT

Select the decompressed ZH-CHT data packs' root path at CDN configuration in the settings, then start the server.

Skip the APP's internal clients download reminder, for the original global version cannot get ZH-CHT data packs after changing language settings and will stuck in an infinite loop. Make sure to manually modify sif2-gl client's CDN settings to match the server's.

Or you can just download it from here : [sif2-gl-zhcnt.apk](https://github.com/HNKServer/Umi/releases/download/1.0/sif2_gl_v13_iap_cdn_fixed.apk)

在设置的 CDN 配置中选择繁体中文数据包解压后的根目录，然后启动服务器。

跳过 APP 内置的客户端下载提示界面，因为原始版本的国际版无法在切换语言设置后正常下载繁体中文数据包，并最终会陷入死循环。确保你手动修改 sif2 国际服客户端版本中的 CDN 设置以匹配服务端。

或者你也可以直接从此处下载：[sif2-gl-zhcnt.apk](https://github.com/HNKServer/Umi/releases/download/1.0/sif2_gl_v13_iap_cdn_fixed.apk)

## Other files required

You can download ZH-CHT data packs from here : [ZH-Android.7z](https://archive.org/download/lovelive-sif2-gl-assets/ZH-Android.7z)

你可以从此处下载繁体中文数据包：[ZH-Android.7z](https://archive.org/download/lovelive-sif2-gl-assets/ZH-Android.7z)

## Credits & Special Thanks

- **Author**: Ethan O'Brien [@ethanaobrien](https://git.ethanthesleepy.one/ethanaobrien)
- **Repository**: [Git - ethanaobrien/ew-android](https://git.ethanthesleepy.one/ethanaobrien/ew-android)
