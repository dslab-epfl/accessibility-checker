<?php
require_once('main.php');
$o = new stdClass();
$o->file = @$_GET['f'] or 'test.pdf';
CheckPDFForAccessibility($o);
echo nl2br($o->msg);
?>