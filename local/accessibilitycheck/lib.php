<?php /**
This plugin is used to check accessibility of PDF documents uploaded by teachers.
When a file is uploaded, it is checked for accessibility
Then, in case of potential problems, a report is displayed in the browser
*/

function course_file_uploaded ($cc) {
if (!isPDFFile($cc->file)) return true;
global $CFG;
require_once($CFG->dirroot . '/local/accessibilitycheck/checker/main.php');
$cc->msg = "Your PDF has been checked for accessibility, and it doesn't seem to have any issue. Congratulations !";
CheckPDFForAccessibility($cc);
return true;
}

/** Check if the given file is effectively a PDF file, by looking at its header */
function isPDFFile ($file) {
$fp = fopen($file, 'rb');
if (!$fp) return false;
$hdr = fread($fp,8);
$result = preg_match('/^%PDF-\d\.\d/i', $hdr);
fclose($fp);
return $result;
}

