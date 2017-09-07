<!DOCTYPE html>
<html>
<head>
    <title>Image</title>
    <#include "/head.ftl">
    <style type="text/css">
        body, html, div {
            width: 100%;
            height: 100%;
        }
        div {
            max-width: 100%;
            display:flex;
            justify-content:center;
            align-items:center;
        }
        img {
            max-width: 100%;
            max-height: 100%;
        }
    </style>
    <meta name="twitter:card" content="photo">
    <meta name="twitter:creator" content="@yawkat">
    <meta name="twitter:creator:id" content="463764901">
    <meta property="og:title" content="Screenshot">
    <meta property="og:type" content="article">
    <meta content="https://s.yawk.at/${id}.twitter.${data.format.defaultExtension}" name="twitter:image">
    <meta content="https://s.yawk.at/${id}" property="og:url">
    <meta content="https://s.yawk.at/${id}.${data.format.defaultExtension}" property="og:image">
    <link href="https://s.yawk.at/${id}.${data.format.defaultExtension}" rel="image_src">
    <meta property="og:site_name" content="yawk.at">
    <meta property="og:description" content="">
</head>
<body>
    <div><img src="${id}.${data.format.defaultExtension}"></div>
</body>
</html>
