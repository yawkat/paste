<!DOCTYPE html>
<html>
<head>
    <title>Video</title>
    <#include "/head.ftl">
    <style type="text/css">
        #svg {
            max-width: 100%;
            display: block;
            margin: 0 auto;
        }
    </style>
    <meta content="noindex" property="robots">
</head>
<body>
    <video id="video" src="/${id}.webm" controls autoplay loop></video>
</body>
</html>
