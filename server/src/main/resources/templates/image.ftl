<html>
<head>
    <title>Image</title>
    <#include "/head.ftl">
    <meta name="twitter:card" content="photo">
    <meta name="twitter:creator" content="@yawkat">
    <meta name="twitter:creator:id" content="463764901">
    <meta property="og:title" content="Screenshot">
    <meta property="og:type" content="article">
    <meta content="http://s.yawk.at/${id}.twitter.${data.format.defaultExtension}" name="twitter:image">
    <meta content="http://s.yawk.at/${id}" property="og:url">
    <meta content="http://s.yawk.at/${id}.${data.format.defaultExtension}" property="og:image">
    <link href="http://s.yawk.at/${id}.${data.format.defaultExtension}" rel="image_src">
    <meta property="og:site_name" content="yawk.at">
    <meta property="og:description" content="">
</head>
<body>
    <img src="${id}.${data.format.defaultExtension}">
</body>
</html>
