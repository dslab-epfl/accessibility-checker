<?php
define('PDFINFO_PATH', 'C:\\wamp\\www\\moodle\\local\\accessibilitycheck\\checker\\pdfinfo.exe');

require_once(dirname(__FILE__).'/msgs.php');

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

function determineGenerator ($info) {
if (isset($info->Creator)) {
if (preg_match('/TeX/', $info->Creator)) return 'latex';
else if (preg_match('/dvips/i', $info->Creator)) return 'latex';
else if (preg_match('/Word|PowerPoint|Excel|Writer|Impress|Calc|Keynote/i', $info->Creator, $m)) return strtolower($m[0]);
else if (preg_match('/PScript5\.dll/i', $info->Creator)) {
if (preg_match('/\.(doc|ppt|xls)x?$/i', $info->Title, $m)) {
$str = strtolower($m[1]);
if ($str=='doc') return 'word';
else if ($str=='ppt') return 'powerpoint';
else if ($str=='xls') return 'excel';
}
else if (preg_match('/Word|PowerPoint|Excel/i', $info->Title, $m)) return strtolower($m[0]);
}
// Other creator-based criterias
}
else if (isset($info->Title)) {
if (preg_match('/\.(docx?|pptx?|xlsx?|tex)$/i', $info->Title, $m)) {
$str = strtolower($m[1]);
if ($str=='doc' || $str=='docx') return 'word';
else if ($str=='ppt' || $str=='pptx') return 'powerpoint';
else if ($str=='xls' || $str=='xlsx') return 'excel';
else if ($str=='tex') return 'latex';
}
// other title-based criterias
}
else if (isset($info->Producer)) {
if (preg_match('/OpenOffice\.org/i', $info->Producer)) return 'writer';
// other producer-based criterias
}
return 'unknown';
}

function CheckPDFForAccessibility ($o) {
$info = pdfinfo($o->file);
if ($info->Tagged==='yes') return true; // For the moment, to keep it simple, consider that a tagged PDF don't need any further check
$gen = determineGenerator($info);

global $AXMSGS;
$msg = $AXMSGS['gNotTagged'];
if ($gen=='word' || $gen=='powerpoint') $msg.="\r\n".$AXMSGS['officeSave'];
else if ($gen=='writer') $msg.="\r\n".$AXMSGS['ooSave'];
$msg.="\r\n".$AXMSGS['gAxTipps'];
if ($gen=='word') $msg.="\r\n".$AXMSGS['axWord'];
else if ($gen=='powerpoint') $msg.="\r\n".$AXMSGS['axPpt'];
else if ($gen=='writer') $msg.="\r\n".$AXMSGS['axOO'];
else if ($gen=='latex') $msg.="\r\n".$AXMSGS['axLatex'];
$o->msg = $msg;
}
?>