<?php
$GLOBALS['AXMSGS'] = array(
'gNotTagged' => <<<'END'
Your PDF document has been checked for accessibility, and has been determined as not being accessible at all.

Your PDF isn't tagged.

To produce a tagged PDF, make sure that the relevant export options are set correctly.

* Use effectively the native "save as", "export"  or plugin features of your editor.
* Never use PDF virtual printers such as PDFCreator, or features like "Print to PDF"; all structural information as well as tagging is lost when this way of creating PDF is used.
* Don't create a PDF directly out of scanned paper. There is no way to access the text of PDF generated that way. If you have no alternative to scanned paper, you must perform an optical character recognition (OCR), and rebuild missing structure yourself after the OCR extracted the text. OCR can recognize text but never structures such as headings, lists, tables, etc.
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
,'axGnr_word' => <<<END
For more information about accessibility in word documents, read
http://webaim.org/techniques/word/
END
,'axGnr_powerpoint' => <<<END
For more information about accessibility in PowerPoint documents, read
http://webaim.org/techniques/powerpoint/
END
,'axGnr_writer' => <<<END
For more information about accessibility in OpenOffice.org/LibreOffice documents, read
http://webaim.org/techniques/ooo/
END
,'axGnr_latex' => <<<END
At this time, there is unfortunately no well known recipee to improve accessibility of PDF documents produced by LaTeX; your document will remain poor in term of accessibility whatever you might do, allthough some packages or combination of packages have a definite good or bad impact. Here are some suggestions to at least not make it even poorer :

* Clearly define the encoding of the source with inputenc package, preferably with latin1 or utf8. This will especially help correctly interpreting greek letters in math equations
* Where possible, use direct character unstead of entities or accenting commands, e.g. "é" unstead of "\\´e"; the later often disturb screen readers that sometimes believe to see two characters unstead of one
* Use hyperref package with unicode option
* Don't use fontenc package; fontenc introduce more ligatures stuff and goes against the accessibility
* You might use pdfcomment package to add alternate text to graphics and formulas; pdfcomment adds a new command \\pdftooltip{content text}{tooltip} which adds a tooltip to the content; these tooltips are read by screen readers

The best way to make accessible documents out of LaTeX is to convert them to HTML unstead of PDF. Look at LaTeX-to-HTML compilers such as HeVeA or LaTeX4ht.
END
,'GTaggedButErrors' => <<<'END'
Your PDF document has been checked for accessibility, and some issues have been found. Fixing them will improve the accessibility of your document, especially if you fix levels 1 and most of level 2 ones.

Problems are classified in three levels of importance:
* Problems of level 1 are critical and may disallow certain users to access important contents of your document. If you have no time, please at least try to fix these ones.
* Problems of level 2 may disallow certain users to access some contents of your document, or may let them to read your document much more comfortably if they are fixed. It would be good if you could fix them, but they are a bit less urgent than level 1.
* Problems of level 3 may help your document to be read more comfortably by some users; you aren't required to fix them, as they are often harder or more time consuming to proceed; but it would be great if you could take a look at them as well.
END
,'tBadAlt' => <<<'END'
Level 1: missing or unappropriate alternate text for figures and/or formulas
Alternate text aka simply alt must be present on all figures and formulas. They must briefly describe what they are attached to, and are important for people who can't see them visually.
* For formulas, alt text should contain a textual representation of the math expression, i.e. corresponding LaTeX code, or like such a formula would be typed in a programming language
* For imagee that are also links or trigger something when clicked, alt text shouldn't describe the image itself but its function, i.e. what a click does, or where the link leads to
* For complex figures like schemas, diagrams, graphics, etc. a full textual description or tables of data used should ideally be present or be referenced at some other place in the document in case the alt is going to be long
* For other images and illustrations, use common sens to make a description that is as short, or as long as it is required to reasonnably understand your document. You can put an empty alt text for images that are purely illustrative (in that case, make the difference between empty but present alt text, allowed and sometimes useful, and total absence of alt text, just bad).

$n unappropriate or missing alt textes have been identified. In more detailled messages further below, you will see exactly at which place they aren't in order (messages MH13-004).
END
,'tBadAlt_word'=><<<'END'
For more information on how to set alternate text of images in word, see http://webaim.org/techniques/word/#alttext
END
,'tBadAlt_powerpoint'=><<<'END'
For more information on how to set alternate text of images in power point slides, see http://webaim.org/techniques/powerpoint/#alttext
END
,'tBadAlt_writer'=><<<'END'
For more information on how to set alternate text of images in open office documents, see http://webaim.org/techniques/ooo/#images
END
,'tBadHeadings' => <<<'END'
Level 1: heading structure mixed up
Headings are very important because they allow users to navigate more quickly in your document, but they can do so only if they have a correct structure. Allthough your document contain headings, they appear to be mixed up or incorrectly nested.
For example, an heading of level 3 shouldn't directly follow an heading of level 1 without any heading of level 2 in between, or otherwise said, you shouldn't skip heading levels. Equally, the first heading of the document must be of level 1.

We found $n headings that appear to be incorrect in such a way. You will see in detailled messages further below at which place exactly there are problems.
END
,'tNoHeadings' => <<<'END'
Level 1: your document doesn't contain any heading
Headings are very important because  they :
* greatly help navigating in your document, even if it contains a table of contents
* allow users to have a quick overview of it at any time thank to automatic anchors
* help web search engines better reference your document
* help you to better organize your thinking

Make sure to use appropriate functionalities of your editor to define headings. In particular, use styling facilities in editors like Word or Open office, and stop using font/size/color quick formatting; making some text bigger makes it an heading visually, but it is still ordinary text for a computer if you don't specifiy it precisely.
In many cases, this issue shows up because of that later reason.
END
,'tNoHeadings_word'=><<<'END'
For more information on how to use styles to add true headings in your word documents, see http://webaim.org/techniques/word/#headings
Note that in word 2007 and above, you can use Alt+Shift+Left/Right to quickly change the heading level of a selected portion of text, as well as Ctrl+Alt+1 to 3 to directly set the style to heading level 1 to 3.
END
,'tNoHeadings_powerpoint'=><<<'END'
For more information on using proper powerpoint slide layout so that headings can be identified, see http://webaim.org/techniques/powerpoint/#layouts
END
,'tNoHeadings_writer'=><<<'END'
For more information on how to use styles to add true headings in your open office writer documents, see http://webaim.org/techniques/ooo/#headings
END
,'tInvalidTagging' => <<<'END'
Level 2: potentially problematic tagging
We found some elements in the structure that are unproperly nested or mixed up in some way that might prevent assistive tools to correctly read your document.
For example :
* A  list must only contain list items, which itself must only contain list labels and list bodies.
* Tables must contain either only rows, or header/body/footer, and table rows must only contain header cells and data cells
* Headings aren't supposed to contain entire paragraphs
* etc.

We found $n points in the structure that might cause problems to assistive tools. Exact locations in your document are shown in detailled messages further below.
END
,'tInvalidTagging_word' => <<<'END'
Unfortunately, if the tagging has been automatically produced by usual text processors like Word, it ma be hard to determine what exactly caused it to generate bad structure. Fixing it in Acrobat Pro or another more advanced tool will probably be needed.
END
,'tInvalidTagging_writer' => <<<'END'
Unfortunately, if the tagging has been automatically produced by text processors like open office, it ma be hard to determine what exactly caused it to generate bad structure. Fixing it in Acrobat Pro or another more advanced tool will probably be needed.
END
,'tInvalidTagging_powerpoint' => <<<'END'
Unfortunately, if the tagging has been automatically produced by usual tools like power point, it ma be hard to determine what exactly caused it to generate bad structure. Fixing it in Acrobat Pro or another more advanced tool will probably be needed.
END
,'tTableNoTh'=><<<'END'
Level 2: tables must have header cells.
All tables should have header cells. An header cell is a cell that describes what is on next cells in the same column or on the same row. Clearly identify header cells allows to :
* Easily deduct how the table is organized
* For screen readers, refer to it to know precisely what is in a given data cell
* If the table continue on multiple pages, repeat them on top or on the left of each page

Usually, the first row, the first column, or both, should be marked as being header cells. In more complex tables, there could be more than one row and/or more than one column of header cells, but this should be avoided as much as possible as it often makes the table harder to read.

If you can't easily identify header cells you have to mark as such, it means that :
* wheither they aren't explicitely written because you supposed that the nature of the data cells were clear enough, in which case we recommand you to add them anyway so that the whole becomes easier to read and understand
* or you used tables to construct a layout disposition rather than to show tabular data, in which case you should try to avoid using tables alltogether for that purpose. Usually you can avoid using tables for layout by using proper alignments, float properties, or other styling features.

$n tables have no header cells in the document. IN detailled messages further below are hints to tell you where exactly it occured.
END
,'tTableNoTh_word'=><<<'END'
Unfortunately, Word doesn't support table headers. To be able to fix this issue, Acrobat Pro or another advanced tool must be used.
END
,'tTableNoTh_powerpoint'=><<<'END'
Unfortunately, PowerPoint doesn't support table headers. To be able to fix this issue, Acrobat Pro or another advanced tool must be used.
END
,'tTableNoTh_writer' =><<<'END'
Refer to http://webaim.org/techniques/ooo/#headers to see how to define table headers in open office documents.
END
,'tTableIrregular'=><<<'END'
Level 3: irregular tables should be avoided.
A table is said to be regular if each row has the same number of columns, and if each column ahs the same number of rows. It becomes irregular as soon as some cells are joined together to make bigger cells, taking the place of more than one normal column or row.
This should be avoided because it often makes the table harder to read, understand, and navigate in. The easiest way to avoid creating irregular tables is by not using fusion or split features once the table and its number of columns is fixed.

$n irregular tables have been found in the document. In detailled messages further below are indications to tell you exactly where.
END
,'tNoLanguageDefined' => <<<'END'
Level 1: the language of the document isn't specified
In order for screen readers to properly read your document, as well as search engines to correctly reference your text, the language must always be specified.
It appears that it isn't the case here. You will usually find the option to define it in general documents properties, or at the same place as spell checking settings.
END
,'tMixedArtifactAndTaggedContents'=><<<'END'
Level 3: mixing artifacts and tagged contents
Artifacts are portions of your document that are visible but have to be ignored by screen readers and search engines because reading or indexing them make no real sens.
The most known form of artifacts are page numbers, as well as repeated page headers and page footers.

Mixing artifacts and tagged contents means that at some place in your document, a zone of regular contents is nested within a zone declared as artifacts, or the opposite. 
This must be avoided because it introduces a doubt for reading softwares: shoulw elements so marked be threated as artifacts or as real contents ?
$n problems of that kind have been encountered.

Unfortunately, fixing this kind of error often require very advanced tools like Acrobat Pro.
END
);//
?>