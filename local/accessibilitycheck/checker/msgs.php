<?php
$AXMSGS = array(
'gNotTagged' => <<<'END'
Your PDF document has been checked for accessibility, and has been determined as not being accessible at all.

Your PDF isn't tagged.
To understand what is PDF tags and why they are important, read
...

To produce a tagged PDF, make sure that the relevant export options are set correctly.

Use effectively the native "save as", "export"  or plugin features of your editor.
Never use PDF virtual printers such as PDFCreator, or features like "Print to PDF"; all structural information as well as tagging is lost when this way of creating PDF is used.
END
,'officeSave' => <<<END
There are multiple ways to produce tagged PDFs from Word or PowerPoint :

* Office 2010, or 2007 with Microsoft PDF plugin
In the Save as dialog box, click "options" and make sure that the boxes to enable accessibility and PDF/A compatibility are both checked.
Afterwards, the settings stay saved once for all, so you don't need to click on "Options" each time you want to create a PDF.

* Office with Adobe add-on
Adobe addon-on come with Acrobat Pro 10+. If you have acrobat, the plugin should have been installed at the same time.
Go to "Preferences" in the Adobe PDF ribbon and make sure that accessibility, tagging and reflowing boxes are checked.
Select "Save as Adobe PDF" in File menu or click on "Create PDF" in Adobe PDF ribbon to save your PDF

* Office 2000 and 2003
It isn't possible to produce tagged PDF files with Office 2000 and 2003. You must upgrade to Office 2007 and install the Microsoft PDF plugin, or use Adobe add-on as described above.

See http://webaim.org/techniques/acrobat/converting for more precise instructions on how to setup your Word or PowerPoint to produce tagged PDF files.
END
,'ooSave' => <<<END
To create tagged PDF files from OpenOffice.org/LibreOffice, choose "Export as PDF" in "File" menu.
In the second dialog box called "PDF Options" that appears after having selected where the file must be saved, make sure that the box "tagged PDF" is checked.

Warning: there exists PDF icons in toolbars or menus that are shortcuts and don't cause the second options dialog box to appear after the first one.
In that case, you don't have any opportunity to check the required box, and it isn't by default. So, don't use those shortcuts.

See http://webaim.org/techniques/acrobat/converting for more precise instructions on how to setup Open Office Writer to produce tagged PDF files.
END
,'gAxTipps' => <<<END
Making a PDF accessible isn't just a matter of tagging. The source document from which the PDF is coming from must also be made accessible, long time before exporting to PDF.

Basic principles includes :
* Provide a consist linear reading order; this is especially important for slide presentations where reading order is not necessarily obvious
* Structure your document with true heading, subheadings and sections, by using style features unstead of quick formatting. A text that is just set to be bigger via quick formatting isn't a real heading, it isn't perceived as such by screen readers and other assistive help softwares. 
* Add alternative text to images, illustrations, graphics, charts, formulas and other non-text elements, so that people who can't see them will still be able to understand
* Use true lists unstead of explicit text or graphical bullets, manual numbering, etc.
* Don't use tables for pure formatting and text disposition on the page; only use tables when you really have to present data in rows and columns
* When presenting data tables, use true tables unstead of false columns, rows and cells created with tabs, manual bounding lines as ascii text, multiple aligned text boxes, and such
END
,'axWord' => <<<END
For more information about accessibility in word documents, read
http://webaim.org/techniques/word/
END
,'axPpt' => <<<END
For more information about accessibility in PowerPoint documents, read
http://webaim.org/techniques/powerpoint/
END
,'axOO' => <<<END
For more information about accessibility in OpenOffice.org/LibreOffice documents, read
http://webaim.org/techniques/ooo/
END
,'axLatex' => <<<END
At this time, there is unfortunately no well known recipee to improve accessibility of PDF documents produced by LaTeX; your document will remain poor in term of accessibility whatever you might do, allthough some packages or combination of packages have a definite good or bad impact. Here are some suggestions to at least not make it even poorer :

* Clearly define the encoding of the source with inputenc package, preferably with latin1 or utf8. This will especially help correctly interpreting greek letters in math equations
* Where possible, use direct character unstead of entities or accenting commands, e.g. "é" unstead of "\\´e"; the later often disturb screen readers that sometimes believe to see two characters unstead of one
* Use hyperref package with unicode option
* Don't use fontenc package; fontenc introduce more ligatures stuff and goes against the accessibility
* You might use pdfcomment package to add alternate text to graphics and formulas; pdfcomment adds a new command \\pdftooltip{content text}{tooltip} which adds a tooltip to the content; these tooltips are read by screen readers
END
);//
?>