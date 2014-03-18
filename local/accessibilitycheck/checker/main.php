<?php
define('PDFINFO_PATH', 'C:\\wamp\\www\\moodle\\local\\accessibilitycheck\\checker\\pdfinfo.exe');

function pdfinfo ($file) {
exec(PDFINFO_PATH .' '. escapeshellarg($file), $lines, $retval);
$o = new stdClass();
foreach($lines as $l) {
list($key, $val) = explode(':', $l);
$key=trim($key);
$val = trim($val);
$o->$key = $val;
}
return $o;
}

function CheckPDFForAccessibility ($o) {
$info = pdfinfo($o->file);
$o->msg = nl2br(print_r($info, true));
}
?>