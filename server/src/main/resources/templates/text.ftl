<!DOCTYPE html>
<html>
<head>
    <title>Code</title>
    <#include "/head.ftl">
<script src="//cdnjs.cloudflare.com/ajax/libs/highlight.js/8.3/highlight.min.js"></script>
<script type="text/javascript">
    // make ctrl+a only select code
    function selectText(el) {
        if (typeof window.getSelection != "undefined" && typeof document.createRange != "undefined") {
            var range = document.createRange();
            range.selectNodeContents(el);
            var sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(range);
        } else if (typeof document.selection != "undefined" && typeof document.body.createTextRange != "undefined") {
            var textRange = document.body.createTextRange();
            textRange.moveToElementText(el);
            textRange.select();
        }
    }
    document.onkeypress = function (e) {
        e = e || window.event;
        if (e.ctrlKey && e.charCode == 97) {
            var block = document.getElementById("code");
            if (block) {
                selectText(block);
                e.preventDefault();
            }
        }
    };
    <#if (data.highlightPreference!"") != "none">
    window.onload = function() {
        var blocks = document.getElementsByTagName("code");
        for (var i = blocks.length - 1; i >= 0; i--) {
            hljs.configure({ useBR: true });
            hljs.highlightBlock(blocks[i]);
        }
    }
    </#if>
</script>
<style type="text/css">
    #code {
        white-space: pre-wrap;
        display: block;
        color: #839496;
        padding-left: 4em;

        font-family: "Source Code Pro", monospace;
        font-size: 16px;

        counter-reset: line;
    }
    #code > span:before {
        content: counter(line, decimal);
        counter-increment: line;
        float: left;
        width: 3em;
        text-align: right;
        margin-right: 1em;
        color: #586e75;

        position: absolute;
        left: 0;
    }
    body {
        background-color: #002b36;
        color: #586e75;
    }
    .hljs {
        display: block;
        overflow-x: auto;
        padding: 0.5em;
        background: #002b36;
        color: #839496;
        -webkit-text-size-adjust: none;
    }
    .hljs,
    /* specify id here to gain position above wrapping elements */
    #code .hljs-title {
        color: #839496;
    }
    .hljs-comment,
    .diff .hljs-header,
    .hljs-doctype,
    .hljs-pi,
    .lisp .hljs-string {
        color: #586e75;
    }
    /* Solarized Green */
    .hljs-winutils,
    .method,
    .hljs-addition,
    .css .hljs-tag,
    .hljs-request,
    .hljs-status,
    .nginx .hljs-title {
        color: #859900;
    }
    /* Solarized Cyan */
    .hljs-command,
    .hljs-string,
    .hljs-tag .hljs-value,
    .hljs-rules .hljs-value,
    .hljs-phpdoc,
    .hljs-dartdoc,
    .tex .hljs-formula,
    .hljs-regexp,
    .hljs-hexcolor,
    .hljs-link_url {
        color: #2aa198;
    }
    /* Solarized Blue */
    .hljs-localvars,
    .hljs-chunk,
    .hljs-decorator,
    .hljs-built_in,
    .hljs-identifier,
    .vhdl .hljs-literal,
    .hljs-id,
    .css .hljs-function {
        color: #268bd2;
    }
    /* Solarized Yellow */
    .hljs-keyword,
    .hljs-attribute,
    .hljs-variable,
    .lisp .hljs-body,
    .smalltalk .hljs-number,
    .hljs-constant,
    .hljs-class .hljs-title,
    .hljs-parent,
    .hljs-type,
    .hljs-link_reference,
    .hljs-annotation {
        color: #b58900;
    }
    /* Solarized Orange */
    .hljs-number,
    .hljs-preprocessor,
    .hljs-preprocessor .hljs-keyword,
    .hljs-pragma,
    .hljs-shebang,
    .hljs-symbol,
    .hljs-symbol .hljs-string,
    .diff .hljs-change,
    .hljs-special,
    .hljs-attr_selector,
    .hljs-subst,
    .hljs-cdata,
    .css .hljs-pseudo,
    .hljs-header {
        color: #cb4b16;
    }
    /* Solarized Red */
    .hljs-deletion,
    .hljs-important,
    .hljs-javadoc {
        color: #dc322f;
    }
    /* Solarized Violet */
    .hljs-link_label {
        color: #6c71c4;
    }
    .tex .hljs-formula {
        background: #073642;
    }
</style>
</head>
<body>
    <code id="code" class="${data.highlightPreference!''?html}"
    ><#list data.lines as line><span>${line?html}<br></span></#list></code>
</body>
</html>
