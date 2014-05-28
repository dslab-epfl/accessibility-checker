<?php
define('PDFINFO_PATH', 'C:\\wamp\\www\\moodle\\local\\accessibilitycheck\\checker\\pdfinfo.exe');
define('TAGGED_ANALYZER_PATH', 'java -jar C:\\wamp\\www\\moodle\\local\\accessibilitycheck\\checker\\analyzer.jar');

require_once(dirname(__FILE__).'/msgs.php');

function pdfinfo ($file) {
exec(PDFINFO_PATH .' '. escapeshellarg($file), $lines, $exitCode);
$o = new stdClass();
foreach($lines as $l) {
@list($key, $val) = explode(':', $l);
$key=trim($key);
$val = trim($val);
if ($key&&$val) $o->$key = $val;
}
$o->exitCode = $exitCode;
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

function taggedAnalysis ($file) {
exec(TAGGED_ANALYZER_PATH .' '. escapeshellarg($file), $lines, $exitCode);
$o = new stdClass();
$o->count = 0;
$o->detail = '';
foreach($lines as $l) {
if ($l&&substr($l,0,2)=='- ') {
$o->count++;
$o->detail .= "$l\r\n";
continue; 
}
@list($key, $val) = explode(':', $l);
$key=trim($key);
$val = trim($val);
if (!$key||!$val) continue;
$o->count++;
$o->$key = $val;
}
$o->exitCode = $exitCode;
return $o;
}

function CheckPDFForAccessibility ($o) {
$msg = '';
$info = pdfinfo($o->file);
if ($info->exitCode!=0) {
$pdfinfopath = PDFINFO_PATH;
$o->msg = <<<END
ERROR: accessibility checking failed, pdfinfo couldn't be run
Phpinfo path: $pdfinfopath
Exit code: {$info->exitCode}
END;
return $o;
}
$gen = determineGenerator($info);
if ($info->Tagged==='yes') {
global $AXMSGS;
$re = taggedAnalysis($o->file);
if ($re->exitCode!=0) {
$path = TAGGED_ANALYZER_PATH;
$o->msg = <<<END
ERROR: accessibility checking failed, java accessibility checker couldn't be run
Analyzer command line: $path
Exit code: {$re->exitCode}
END;
return $o;
}
if ($re->count>0) {
$msg = $AXMSGS['GTaggedButErrors'];
foreach ($re as $key=>$num) {
if (isset($AXMSGS["t$key"])) $msg .= "\r\n\r\n" .str_replace('$n', $num, $AXMSGS["t$key"]);
if (isset($AXMSGS["t{$key}_{$gen}"])) $msg .= "\r\n\r\n" .$AXMSGS["t{$key}_{$gen}"];
}
//$msg .= "\r\n\r\n" .$AXMSGS['gAxTipps'];
if (isset($AXMSGS["axGnr_$gen"])) $msg.="\r\n\r\n".$AXMSGS["axGnr_$gen"];
$msg .= "\r\n\r\nDetailled messages :\r\n" .$re->detail;
//$o->msg = $msg;
}}
else {
global $AXMSGS;
$msg = $AXMSGS['gNotTagged'];
if ($gen=='word' || $gen=='powerpoint') $msg.="\r\n".$AXMSGS['officeSave'];
else if ($gen=='writer') $msg.="\r\n".$AXMSGS['ooSave'];
$msg.="\r\n".$AXMSGS['gAxTipps']."\r\n";
if (isset($AXMSGS["axGnr_$gen"])) $msg.="\r\n".$AXMSGS["axGnr_$gen"];
}
if (isset($msg) && $msg) $o->msg = utf8_encode(nl2br(htmlspecialchars($msg, ENT_IGNORE, 'ISO-8859-1')));
@file_put_contents("C:\\wamp\\www\\moodle\\local\\accessibilitycheck\\checker\\msg.log", $o->msg);
}
?>