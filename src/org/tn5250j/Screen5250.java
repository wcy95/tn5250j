/**
 *
 * <p>
 * Title: Screen5250.java
 * </p>
 * <p>
 * Description: Main interface to draw the graphical image of the screen
 * </p>
 * <p>
 * Copyright: Copyright (c) 2000 - 2002
 * </p>
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this software; see the file COPYING. If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * </p>
 *
 * @author Kenneth J. Pouncey
 * @version 0.5
 */
package org.tn5250j;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.text.*;
import java.util.*;
import java.awt.datatransfer.*;
import java.beans.*;

import org.tn5250j.tools.logging.*;
import org.tn5250j.tools.*;
import org.tn5250j.tools.system.OperatingSystem;
import org.tn5250j.event.ScreenListener;

public class Screen5250 implements PropertyChangeListener, TN5250jConstants,
		ActionListener {

//	ScreenChar[] screen;
	private ScreenChar[] errorLine;
	private ScreenFields screenFields;
	private int errorLineNum;
	public Font font;
	private int lastAttr;
	private int lastRow;
	private int lastCol;
	private int lastPos;
	private int lenScreen;
	protected GuiGraphicBuffer bi;
	private KeyStrokenizer strokenizer;
	private tnvt sessionVT;
	private int numRows = 0;
	private int numCols = 0;
	int fmWidth = 0;
	int fmHeight = 0;
	LineMetrics lm;
	Color colorBlue;
	Color colorWhite;
	Color colorRed;
	Color colorGreen;
	Color colorPink;
	Color colorYellow;
	Color colorBg;
	Color colorTurq;
	Color colorGUIField;
	Color colorCursor;
	Color colorSep;
	Color colorHexAttr;
	private boolean updateCursorLoc;
	private Rectangle2D cursor = new Rectangle2D.Float();
	private Rectangle2D tArea; // text area
	private Rectangle2D aArea; // all screen area
	private Rectangle2D cArea; // command line area
	private Rectangle2D sArea; // status area
	private char char0 = 0;
	private static final int initAttr = 32;
	private static final char initChar = 0;
	private boolean statusErrorCode;
	private boolean statusXSystem;
	private int top;
	private int left;
	private Rectangle workR = new Rectangle();
   private int colSepLine = 0;
	public boolean cursorActive = false;
	public boolean cursorShown = false;
	protected boolean insertMode = false;
	private boolean keyProcessed = false;
	private Rectangle dirty = new Rectangle();
	private Graphics2D g2d;
	protected Graphics2D gg2d;
	private Point startPoint;
	private Point endPoint;
	protected int crossHair = 0;
	private boolean messageLight = false;
	public int homePos = 0;
	public int saveHomePos = 0;
	private String bufferedKeys;
	private boolean updateFont;
	public boolean pendingInsert = false;
	private Gui5250 gui;
	protected int cursorSize = 0;
	private boolean hotSpots = false;
	private boolean showHex = false;
	private float sfh = 1.2f; // font scale height
	private float sfw = 1.0f; // font scale height
	private float ps132 = 0; // Font point size

	public final static byte STATUS_SYSTEM = 1;
	public final static byte STATUS_ERROR_CODE = 2;
	public final static byte STATUS_VALUE_ON = 1;
	public final static byte STATUS_VALUE_OFF = 2;

	private StringBuffer hsMore = new StringBuffer("More...");
	private StringBuffer hsBottom = new StringBuffer("Bottom");

	// error codes to be sent to the host on an error
	private final static int ERR_CURSOR_PROTECTED = 0x05;
	private final static int ERR_INVALID_SIGN = 0x11;
	private final static int ERR_NO_ROOM_INSERT = 0x12;
	private final static int ERR_NUMERIC_ONLY = 0x09;
	private final static int ERR_DUP_KEY_NOT_ALLOWED = 0x19;
	private final static int ERR_NUMERIC_09 = 0x10;
	private final static int ERR_FIELD_MINUS = 0x16;
	private final static int ERR_FIELD_EXIT_INVALID = 0x18;
	private final static int ERR_ENTER_NO_ALLOWED = 0x20;
	private final static int ERR_MANDITORY_ENTER = 0x21;

	private boolean guiInterface = false;
	protected boolean guiShowUnderline = true;
	private boolean restrictCursor = false;
	private Rectangle restriction;
	private boolean resetRequired;
	protected int cursorBottOffset;
	private boolean defaultPrinter;
	private SessionConfig config;
	protected boolean rulerFixed;
	private boolean antialiased = true;
	private boolean feError;

   // vector of listeners for changes to the screen.
   Vector listeners = null;

   // Operator Information Area
   private ScreenOIA oia;

   // screen planes
   protected ScreenPlanes planes;

	//Added by Barry
	private StringBuffer keybuf;

	private boolean fullRepaint;

	//   private Image tileimage;

	//LDC - 12/02/2003 - boolean: true: it must be repainted
	private boolean drawing;

	private javax.swing.Timer blinker;

	private TN5250jLogger log = TN5250jLogFactory.getLogger(this.getClass());

	public Screen5250(Gui5250 gui, SessionConfig config) {

		//      ImageIcon ic = new ImageIcon("transtable1.jpg");
		//      tileimage = ic.getImage();

		//Added by Barry
		this.keybuf = new StringBuffer();

		this.gui = gui;

		this.config = config;

		// load the session properties from it's profile.
		loadProps();

		try {
			jbInit();
		} catch (Exception ex) {
			log.warn("In constructor: ", ex);
		}
	}

	void jbInit() throws Exception {

		// damn I hate putting this in but it is the only way to get
		//  it to work correctly. What a pain in the ass.
		if (OperatingSystem.isMacOS() && OperatingSystem.hasJava14())
			fullRepaint = true;

		// change by Luc - LDC If the font from the properties file does not
		// exist
		//    select the default font
		String fontName = null;
		if (config.isPropertyExists("font")) {
			fontName = getStringProperty("font");
			if (GUIGraphicsUtils.isFontNameExists(fontName) == false)
				fontName = null;
		}

		//      if (!config.isPropertyExists("font")) {
		if (fontName == null) {
			font = new Font(GUIGraphicsUtils.getDefaultFont(), Font.PLAIN, 14);
			//         font = new Font("Courier New",Font.PLAIN,14);
			config.setProperty("font", font.getFontName());
		} else {
			//font = new Font(getStringProperty("font"),Font.PLAIN,14);
			font = new Font(fontName, Font.PLAIN, 14);
		}

		gui.setFont(font);

		lastAttr = 32;

		// default number of rows and columns
		numRows = 24;
		numCols = 80;

		goto_XY(1, 1); // set initial cursor position

		restriction = new Rectangle(0, 0);

		errorLineNum = numRows;
		updateCursorLoc = false;
		FontRenderContext frc = new FontRenderContext(font.getTransform(),
				true, true);
		lm = font.getLineMetrics("Wy", frc);
		fmWidth = (int) font.getStringBounds("W", frc).getWidth() + 1;
		fmHeight = (int) (font.getStringBounds("g", frc).getHeight()
				+ lm.getDescent() + lm.getLeading());

      oia = new ScreenOIA(this);
      oia.setKeyBoardLocked(true);

		checkOffScreenImage();
		lenScreen = numRows * numCols;

      planes = new ScreenPlanes(this,numRows);

//		screen = new ScreenChar[lenScreen];
//
//		for (int y = 0; y < lenScreen; y++) {
//			screen[y] = new ScreenChar(this);
//			screen[y].setCharAndAttr(' ', initAttr, false);
//			screen[y].setRowCol(getRow(y), getCol(y));
//		}

		screenFields = new ScreenFields(this);
		strokenizer = new KeyStrokenizer();


	}

   public ScreenPlanes getPlanes() {
      return planes;
   }

   public final ScreenOIA getOIA() {
      return oia;
   }

	public final void setRowsCols(int rows, int cols) {

		// default number of rows and columns
		numRows = rows;
		numCols = cols;

		lenScreen = numRows * numCols;

//		screen = new ScreenChar[lenScreen];
//
//		for (int y = 0; y < lenScreen; y++) {
//			screen[y] = new ScreenChar(this);
//			screen[y].setCharAndAttr(' ', initAttr, false);
//			screen[y].setRowCol(getRow(y), getCol(y));
//		}
		errorLineNum = numRows;

      planes.setSize(rows);

		Rectangle r = gui.getDrawingBounds();
		resizeScreenArea(r.width, r.height);
		gui.repaint();
	}

	public void loadProps() {

		loadColors();

		if (config.isPropertyExists("colSeparator")) {
			if (getStringProperty("colSeparator").equals("Line"))
				colSepLine = 0;
			if (getStringProperty("colSeparator").equals("ShortLine"))
				colSepLine = 1;
			if (getStringProperty("colSeparator").equals("Dot"))
				colSepLine = 2;
			if (getStringProperty("colSeparator").equals("Hide"))
				colSepLine = 3;
		}

		if (config.isPropertyExists("showAttr")) {
			if (getStringProperty("showAttr").equals("Hex"))
				showHex = true;
		}

		if (config.isPropertyExists("guiInterface")) {
			if (getStringProperty("guiInterface").equals("Yes"))
				guiInterface = true;
			else
				guiInterface = false;
		}

		if (config.isPropertyExists("guiShowUnderline")) {
			if (getStringProperty("guiShowUnderline").equals("Yes"))
				guiShowUnderline = true;
			else
				guiShowUnderline = false;
		}

		if (config.isPropertyExists("hotspots")) {
			if (getStringProperty("hotspots").equals("Yes"))
				hotSpots = true;
			else
				hotSpots = false;
		}

		if (config.isPropertyExists("hsMore")) {
			if (getStringProperty("hsMore").length() > 0) {
				hsMore.setLength(0);
				hsMore.append(getStringProperty("hsMore"));
			}
		}

		if (config.isPropertyExists("hsBottom")) {
			if (getStringProperty("hsBottom").length() > 0) {
				hsBottom.setLength(0);
				hsBottom.append(getStringProperty("hsBottom"));
			}
		}

		if (config.isPropertyExists("colSeparator")) {
			if (getStringProperty("colSeparator").equals("Line"))
				colSepLine = 0;
			if (getStringProperty("colSeparator").equals("ShortLine"))
				colSepLine = 1;
			if (getStringProperty("colSeparator").equals("Dot"))
				colSepLine = 2;
			if (getStringProperty("colSeparator").equals("Hide"))
				colSepLine = 3;
		}

		if (config.isPropertyExists("cursorSize")) {
			if (getStringProperty("cursorSize").equals("Full"))
				cursorSize = 2;
			if (getStringProperty("cursorSize").equals("Half"))
				cursorSize = 1;
			if (getStringProperty("cursorSize").equals("Line"))
				cursorSize = 0;

		}

		if (config.isPropertyExists("crossHair")) {
			if (getStringProperty("crossHair").equals("None"))
				crossHair = 0;
			if (getStringProperty("crossHair").equals("Horz"))
				crossHair = 1;
			if (getStringProperty("crossHair").equals("Vert"))
				crossHair = 2;
			if (getStringProperty("crossHair").equals("Both"))
				crossHair = 3;

		}

		if (config.isPropertyExists("rulerFixed")) {

			if (getStringProperty("rulerFixed").equals("Yes"))
				rulerFixed = true;
			else
				rulerFixed = false;

		}

		if (config.isPropertyExists("fontScaleHeight")) {
			sfh = getFloatProperty("fontScaleHeight");
		}

		if (config.isPropertyExists("fontScaleWidth")) {
			sfw = getFloatProperty("fontScaleWidth");
		}

		if (config.isPropertyExists("fontPointSize")) {
			ps132 = getFloatProperty("fontPointSize");
		}

		if (config.isPropertyExists("cursorBottOffset")) {
			cursorBottOffset = getIntProperty("cursorBottOffset");
		}

		if (config.isPropertyExists("defaultPrinter")) {
			if (getStringProperty("defaultPrinter").equals("Yes"))
				defaultPrinter = true;
			else
				defaultPrinter = false;
		}

		if (config.isPropertyExists("resetRequired")) {
			if (getStringProperty("resetRequired").equals("Yes"))
				resetRequired = true;
			else
				resetRequired = false;
		}

		if (config.isPropertyExists("useAntialias")) {

			if (getStringProperty("useAntialias").equals("Yes"))
				antialiased = true;
			else
				antialiased = false;

		}

		if (config.getStringProperty("cursorBlink").equals("Yes")) {
			blinker = new javax.swing.Timer(500, this);
			blinker.start();
		}
	}

	protected final void loadColors() {

		colorBlue = new Color(140, 120, 255);
		colorTurq = new Color(0, 240, 255);
		colorRed = Color.red;
		colorWhite = Color.white;
		colorYellow = Color.yellow;
		colorGreen = Color.green;
		colorPink = Color.magenta;
		colorGUIField = Color.white;
		colorSep = Color.white;
		colorHexAttr = Color.white;

		if (guiInterface)
			colorBg = Color.lightGray;
		else
			colorBg = Color.black;

		colorCursor = Color.white;

		if (!config.isPropertyExists("colorBg"))
			setProperty("colorBg", Integer.toString(colorBg.getRGB()));
		else {
			colorBg = getColorProperty("colorBg");
		}
		gui.setBackground(colorBg);

		if (!config.isPropertyExists("colorBlue"))
			setProperty("colorBlue", Integer.toString(colorBlue.getRGB()));
		else
			colorBlue = getColorProperty("colorBlue");

		if (!config.isPropertyExists("colorTurq"))
			setProperty("colorTurq", Integer.toString(colorTurq.getRGB()));
		else
			colorTurq = getColorProperty("colorTurq");

		if (!config.isPropertyExists("colorRed"))
			setProperty("colorRed", Integer.toString(colorRed.getRGB()));
		else
			colorRed = getColorProperty("colorRed");

		if (!config.isPropertyExists("colorWhite"))
			setProperty("colorWhite", Integer.toString(colorWhite.getRGB()));
		else
			colorWhite = getColorProperty("colorWhite");

		if (!config.isPropertyExists("colorYellow"))
			setProperty("colorYellow", Integer.toString(colorYellow.getRGB()));
		else
			colorYellow = getColorProperty("colorYellow");

		if (!config.isPropertyExists("colorGreen"))
			setProperty("colorGreen", Integer.toString(colorGreen.getRGB()));
		else
			colorGreen = getColorProperty("colorGreen");

		if (!config.isPropertyExists("colorPink"))
			setProperty("colorPink", Integer.toString(colorPink.getRGB()));
		else
			colorPink = getColorProperty("colorPink");

		if (!config.isPropertyExists("colorGUIField"))
			setProperty("colorGUIField", Integer.toString(colorGUIField
					.getRGB()));
		else
			colorGUIField = getColorProperty("colorGUIField");

		if (!config.isPropertyExists("colorCursor"))
			setProperty("colorCursor", Integer.toString(colorCursor.getRGB()));
		else
			colorCursor = getColorProperty("colorCursor");

		if (!config.isPropertyExists("colorSep")) {
			colorSep = colorWhite;
			setProperty("colorSep", Integer.toString(colorSep.getRGB()));
		} else
			colorSep = getColorProperty("colorSep");

		if (!config.isPropertyExists("colorHexAttr")) {
			colorHexAttr = colorWhite;
			setProperty("colorHexAttr", Integer.toString(colorHexAttr.getRGB()));
		} else
			colorHexAttr = getColorProperty("colorHexAttr");

	}

	/**
	 * This is for blinking cursor but should be moved out
	 */
	public void actionPerformed(ActionEvent actionevent) {
		if (actionevent.getSource() instanceof javax.swing.Timer) {

			//         if (!cursorActive)
			//            return;
			//
			//         if (cursorShown)
			//            setCursorOff();
			//         else
			//            setCursorOn();
			if (cursorActive)
				setCursorActive(false);
			else
				setCursorActive(true);
		}
	}

	protected final String getStringProperty(String prop) {

		return config.getStringProperty(prop);

	}

	protected final Color getColorProperty(String prop) {

		return config.getColorProperty(prop);

	}

	protected final float getFloatProperty(String prop) {

		return config.getFloatProperty(prop);

	}

	protected final int getIntProperty(String prop) {

		return config.getIntegerProperty(prop);

	}

	protected final void setProperty(String key, String val) {

		config.setProperty(key, val);

	}

	public void propertyChange(PropertyChangeEvent pce) {

		String pn = pce.getPropertyName();
		boolean resetAttr = false;

		if (pn.equals("colorBg")) {
			colorBg = (Color) pce.getNewValue();
			resetAttr = true;

		}

		if (pn.equals("colorBlue")) {
			colorBlue = (Color) pce.getNewValue();
			resetAttr = true;
		}

		if (pn.equals("colorTurq")) {
			colorTurq = (Color) pce.getNewValue();
			resetAttr = true;
		}

		if (pn.equals("colorRed")) {
			colorRed = (Color) pce.getNewValue();
			resetAttr = true;
		}

		if (pn.equals("colorWhite")) {
			colorWhite = (Color) pce.getNewValue();
			resetAttr = true;
		}

		if (pn.equals("colorYellow")) {
			colorYellow = (Color) pce.getNewValue();
			resetAttr = true;
		}

		if (pn.equals("colorGreen")) {
			colorGreen = (Color) pce.getNewValue();
			resetAttr = true;
		}

		if (pn.equals("colorPink")) {
			colorPink = (Color) pce.getNewValue();
			resetAttr = true;
		}

		if (pn.equals("colorGUIField")) {
			colorGUIField = (Color) pce.getNewValue();
			resetAttr = true;
		}

		if (pn.equals("colorCursor")) {
			colorCursor = (Color) pce.getNewValue();
			resetAttr = true;
		}

		if (pn.equals("colorSep")) {
			colorSep = (Color) pce.getNewValue();
			resetAttr = true;
		}

		if (pn.equals("colorHexAttr")) {
			colorHexAttr = (Color) pce.getNewValue();
			resetAttr = true;
		}

		if (pn.equals("cursorSize")) {
			if (pce.getNewValue().equals("Full"))
				cursorSize = 2;
			if (pce.getNewValue().equals("Half"))
				cursorSize = 1;
			if (pce.getNewValue().equals("Line"))
				cursorSize = 0;

		}

		if (pn.equals("crossHair")) {
			if (pce.getNewValue().equals("None"))
				crossHair = 0;
			if (pce.getNewValue().equals("Horz"))
				crossHair = 1;
			if (pce.getNewValue().equals("Vert"))
				crossHair = 2;
			if (pce.getNewValue().equals("Both"))
				crossHair = 3;
		}

		if (pn.equals("rulerFixed")) {
			if (pce.getNewValue().equals("Yes"))
				rulerFixed = true;
			else
				rulerFixed = false;
		}

		if (pn.equals("colSeparator")) {
			if (pce.getNewValue().equals("Line"))
				colSepLine = 0;
			if (pce.getNewValue().equals("ShortLine"))
				colSepLine = 1;
			if (pce.getNewValue().equals("Dot"))
				colSepLine = 2;
			if (pce.getNewValue().equals("Hide"))
				colSepLine = 3;
		}

		if (pn.equals("showAttr")) {
			if (pce.getNewValue().equals("Hex"))
				showHex = true;
			else
				showHex = false;
		}

		if (pn.equals("guiInterface")) {
			if (pce.getNewValue().equals("Yes"))
				guiInterface = true;
			else
				guiInterface = false;
		}

		if (pn.equals("guiShowUnderline")) {
			if (pce.getNewValue().equals("Yes"))
				guiShowUnderline = true;
			else
				guiShowUnderline = false;
		}

		if (pn.equals("hotspots")) {
			if (pce.getNewValue().equals("Yes"))
				hotSpots = true;
			else
				hotSpots = false;
		}

		if (pn.equals("defaultPrinter")) {
			if (pce.getNewValue().equals("Yes"))
				defaultPrinter = true;
			else
				defaultPrinter = false;
		}

		if (pn.equals("resetRequired")) {
			if (pce.getNewValue().equals("Yes"))
				resetRequired = true;
			else
				resetRequired = false;
		}

		if (pn.equals("hsMore")) {
			hsMore.setLength(0);
			hsMore.append((String) pce.getNewValue());

		}

		if (pn.equals("hsBottom")) {
			hsBottom.setLength(0);
			hsBottom.append((String) pce.getNewValue());

		}

		if (pn.equals("font")) {
			font = new Font((String) pce.getNewValue(), Font.PLAIN, 14);
			updateFont = true;
		}

		if (pn.equals("useAntialias")) {
			if (pce.getNewValue().equals("Yes"))
				bi.setUseAntialias(true);
			else
				bi.setUseAntialias(false);
			updateFont = true;
		}

		if (pn.equals("fontScaleHeight")) {

			//         try {
			sfh = Float.parseFloat((String) pce.getNewValue());
			updateFont = true;
			//         }

		}

		if (pn.equals("fontScaleWidth")) {

			//         try {
			sfw = Float.parseFloat((String) pce.getNewValue());
			updateFont = true;
			//         }

		}

		if (pn.equals("fontPointSize")) {

			//         try {
			ps132 = Float.parseFloat((String) pce.getNewValue());
			updateFont = true;
			//         }

		}

		if (pn.equals("cursorBottOffset")) {
			cursorBottOffset = getIntProperty("cursorBottOffset");
		}

		if (pn.equals("cursorBlink")) {

			log.debug(getStringProperty("cursorBlink"));
			if (pce.getNewValue().equals("Yes")) {

				if (blinker == null) {

					blinker = new javax.swing.Timer(500, this);
					blinker.start();
				}
			} else {

				if (blinker != null) {
					blinker.stop();
					blinker = null;
				}
			}
		}

		if (updateFont) {
			Rectangle r = gui.getDrawingBounds();
			resizeScreenArea(r.width, r.height);
			updateFont = false;
		}

		if (resetAttr) {
//			for (int y = 0; y < lenScreen; y++) {
//				screen[y].setAttribute(screen[y].getCharAttr());
//			}
			bi.drawOIA();
		}
		gui.validate();
		gui.repaint();
	}

	public void setBlinkCursorStop() {
		if (blinker != null) {
			blinker.stop();
			blinker.removeActionListener(this);
			blinker = null;
		}
	}

	public boolean isBlinkCursor() {

		return blinker != null;

	}

	public boolean isHotSpots() {
		return hotSpots;
	}

	public void toggleHotSpots() {
		hotSpots = !hotSpots;
	}

	public void toggleGUIInterface() {
		guiInterface = !guiInterface;
	}

	/**
	 *
	 * RubberBanding start code
	 *
	 */

	/**
	 * Translate the starting point of mouse movement to encompass a full
	 * character
	 *
	 * @param start
	 * @return Point
	 */
	public Point translateStart(Point start) {

		// because getRowColFromPoint returns position offset as 1,1 we need
		// to translate as offset 0,0
		int pos = getPosFromView(start.x, start.y);
      int x = fmWidth * getCol(pos);
      int y = fmHeight * getRow(pos);
//		start.setLocation(screen[pos].x, screen[pos].y);
		start.setLocation(x, y);
		return start;

	}

	/**
	 * Translate the ending point of mouse movement to encompass a full
	 * character
	 *
	 * @param end
	 * @return Point
	 */
	public Point translateEnd(Point end) {

		// because getRowColFromPoint returns position offset as 1,1 we need
		// to translate as offset 0,0
		int pos = getPosFromView(end.x, end.y);
//		if (pos >= screen.length) {
//			pos = screen.length - 1;
//		}
//		int x = screen[pos].x + fmWidth - 1;
//		int y = screen[pos].y + fmHeight - 1;

		if (pos >= lenScreen) {
			pos = lenScreen - 1;
		}
		int x =  ((fmWidth * getCol(pos)) + fmWidth) - 1;
		int y = ((fmHeight * getRow(pos)) + fmHeight) - 1;

		end.setLocation(x, y);

		return end;
	}

	/**
	 *
	 * RubberBanding end code
	 *
	 */

	/**
	 *
	 * Copy & Paste start code
	 *
	 */
	public final void copyMe() {

		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringBuffer s = new StringBuffer();

		// lets get the bounding area using a rectangle that we have already
		// allocated
		getBoundingArea(workR);

		gui.rubberband.reset();
		gui.repaint();

		log.debug("Copying" + workR);

		// loop through all the screen characters to send them to the clip board
		int m = workR.x;
		int i = 0;
		int t = 0;

		while (workR.height-- > 0) {
			t = workR.width;
			i = workR.y;
			while (t-- > 0) {
				// only copy printable characters (in this case >= ' ')
//				char c = screen[getPos(m - 1, i - 1)].getChar();
				char c = planes.getChar(getPos(m - 1, i - 1));
//				if (c >= ' ' && !screen[getPos(m - 1, i - 1)].nonDisplay)
            // TODO: change me here to implement the nondisplay check later
				if (c >= ' ')
					s.append(c);
				else
					s.append(' ');

				i++;
			}
			s.append('\n');
			m++;
		}
		StringSelection contents = new StringSelection(s.toString());
		cb.setContents(contents, null);

	}

	protected final void pasteMe(boolean special) {

		setCursorActive(false);
		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable content = cb.getContents(this);
		try {
			StringBuffer sb = new StringBuffer((String) content
					.getTransferData(DataFlavor.stringFlavor));
			StringBuffer pd = new StringBuffer();
			int r = getRow(lastPos);
			int nextChar = 0;
			int nChars = sb.length();
			boolean omitLF = false;
			boolean done = false;
			screenFields.saveCurrentField();
			int lr = getRow(lastPos);
			int lc = getCol(lastPos);
			resetDirty(lastPos);

			while (!done) {

				if (nextChar >= nChars) { /* EOF */

					done = true;
					break;
				}

				pd.setLength(0);

				boolean eol = false;
				char c = 0;
				int i;

				/* Skip a leftover '\n', if necessary */
				if (omitLF && (sb.charAt(nextChar) == '\n'))
					nextChar++;

				boolean skipLF = false;
				omitLF = false;

				charLoop:

				for (i = nextChar; i < nChars; i++) {
					c = sb.charAt(i);
					if ((c == '\n') || (c == '\r')) {
						eol = true;
						break charLoop;
					}
				}

				int startChar = nextChar;
				nextChar = i;

				pd.append(sb.substring(startChar, startChar + (i - startChar)));

				if (eol) {
					nextChar++;
					if (c == '\r') {
						skipLF = true;
					}
				}
				log.debug("pasted >" + pd + "<");

				int col = getCol(lastPos);
				int t = numCols - col;
				if (t > pd.length())
					t = pd.length();
				int p = 0;
				char pc;
				boolean setIt;
				while (t-- > 0) {

					pc = pd.charAt(p);
					setIt = true;
					if (special
							&& (!Character.isLetter(pc) && !Character
									.isDigit(pc)))
						setIt = false;

					if (isInField(r, col) && setIt) {
						//screen[getPos(r, col)].setChar(pc);
						planes.setChar(getPos(r, col), pc);
						setDirty(r, col);
						screenFields.setCurrentFieldMDT();
					}
					p++;
					if (setIt)
						col++;
				}
				r++;

			}
			screenFields.restoreCurrentField();
			updateDirty();

			goto_XY(lr + 1, lc + 1);

			setCursorActive(true);
		} catch (Throwable exc) {
			log.warn("" + exc.getMessage());
		}
	}

	protected final void copyField(int pos) {

		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringBuffer s = new StringBuffer();
		screenFields.saveCurrentField();
		isInField(pos);
		log.debug("Copying");
		StringSelection contents = new StringSelection(screenFields
				.getCurrentFieldText());
		cb.setContents(contents, null);
		screenFields.restoreCurrentField();
	}

	/**
	 * Fills the passed Rectangle with the starting row and column and width and
	 * height of the selected area.
	 *
	 * 1 BASED so column 1 row one is returned 1,1
	 *
	 * If there is no area bounded then the full screen area is returned.
	 *
	 * @param bounds
	 */
	public void getBoundingArea(Rectangle bounds) {

		// check to see if there is an area selected. If not then return all
		//    screen area.
		if (!gui.rubberband.isAreaSelected()) {

			bounds.setBounds(1, 1, getCols(), getRows());
		} else {
			// lets get the bounding area using a rectangle that we have already
			// allocated
			gui.rubberband.getBoundingArea(workR);

			// get starting row and column
			int sPos = getRowColFromPoint(workR.x, workR.y);
			// get the width and height
			int ePos = getRowColFromPoint(workR.width, workR.height);

			int row = getRow(sPos) + 1;
			int col = getCol(sPos) + 1;

			bounds.setBounds(row, col, getCol(ePos) + 1, getRow(ePos) + 1);
		}
	}

	/**
	 *
	 * Copy & Paste end code
	 *
	 */

	/**
	 * Sum them
	 *
	 * @param which
	 *            formatting option to use
	 * @return vector string of numberic values
	 */
	protected final Vector sumThem(boolean which) {

		StringBuffer s = new StringBuffer();

		getBoundingArea(workR);

		//      gui.rubberband.reset();
		//      gui.repaint();

		log.debug("Summing");

		// obtain the decimal format for parsing
		DecimalFormat df = (DecimalFormat) NumberFormat.getInstance();

		DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();

		if (which) {
			dfs.setDecimalSeparator('.');
			dfs.setGroupingSeparator(',');
		} else {
			dfs.setDecimalSeparator(',');
			dfs.setGroupingSeparator('.');
		}

		df.setDecimalFormatSymbols(dfs);

		Vector sumVector = new Vector();

		// loop through all the screen characters to send them to the clip board
		int m = workR.x;
		int i = 0;
		int t = 0;

		double sum = 0.0;

		while (workR.height-- > 0) {
			t = workR.width;
			i = workR.y;
			while (t-- > 0) {

				// only copy printable numeric characters (in this case >= ' ')
//				char c = screen[getPos(m - 1, i - 1)].getChar();
				char c = planes.getChar(getPos(m - 1, i - 1));
//				if (((c >= '0' && c <= '9') || c == '.' || c == ',' || c == '-')
//						&& !screen[getPos(m - 1, i - 1)].nonDisplay) {

            // TODO: update me here to implement the nonDisplay check as well
				if (((c >= '0' && c <= '9') || c == '.' || c == ',' || c == '-')) {
					s.append(c);
				}
				i++;
			}

			if (s.length() > 0) {
				if (s.charAt(s.length() - 1) == '-') {
					s.insert(0, '-');
					s.deleteCharAt(s.length() - 1);
				}
				try {
					Number n = df.parse(s.toString());
					//               System.out.println(s + " " + n.doubleValue());

					sumVector.add(new Double(n.doubleValue()));
					sum += n.doubleValue();
				} catch (ParseException pe) {
					System.out.println(pe.getMessage() + " at "
							+ pe.getErrorOffset());
				}
			}
			s.setLength(0);
			m++;
		}
		log.debug("" + sum);
		return sumVector;
	}

	public void moveCursor(MouseEvent e) {
		if (!oia.isKeyBoardLocked()) {

			int pos = getPosFromView(e.getX(), e.getY());
			if (log.isDebugEnabled()) {
				log.debug((getRow(pos)) + "," + (getCol(pos)));
				log.debug(e.getX() + "," + e.getY() + "," + fmWidth + ","
						+ fmHeight);
			}
			if (pos < 0)
				return;
			// because getRowColFromPoint returns offset of 1,1 we need to
			//    translate to offset 0,0
			//         pos -= (numCols + 1);

//			int g = screen[pos].getWhichGUI();
         int g = planes.getWhichGUI(pos);

			// lets check for hot spots
			if (g >= ScreenChar.BUTTON_LEFT && g <= ScreenChar.BUTTON_LAST) {
				StringBuffer aid = new StringBuffer();
				boolean aidFlag = true;
				switch (g) {
				case ScreenChar.BUTTON_RIGHT:
				case ScreenChar.BUTTON_MIDDLE:
//					while (screen[--pos].getWhichGUI() != ScreenChar.BUTTON_LEFT) {
					while (planes.getWhichGUI(--pos) != ScreenChar.BUTTON_LEFT) {
					}
				case ScreenChar.BUTTON_LEFT:
//					if (screen[pos].getChar() == 'F') {
					if (planes.getChar(pos) == 'F') {
						pos++;
					} else
						aidFlag = false;

//					if (screen[pos + 1].getChar() != '='
//							&& screen[pos + 1].getChar() != '.'
//							&& screen[pos + 1].getChar() != '/') {
					if (planes.getChar(pos + 1) != '='
							&& planes.getChar(pos + 1) != '.'
							&& planes.getChar(pos + 1) != '/') {
						//                     System.out.println(" Hotspot clicked!!! we will send
						// characters " +
						//                                    screen[pos].getChar() +
						//                                    screen[pos+1].getChar());
						aid.append(planes.getChar(pos));
						aid.append(planes.getChar(pos + 1));
					} else {
//						log.debug(" Hotspot clicked!!! we will send character "
//								+ screen[pos].getChar());
						log.debug(" Hotspot clicked!!! we will send character "
								+ planes.getChar(pos));
						//
						aid.append(planes.getChar(pos));
					}
					break;

				}
				if (aidFlag) {
					switch (g) {

					case ScreenChar.BUTTON_LEFT_UP:
					case ScreenChar.BUTTON_MIDDLE_UP:
					case ScreenChar.BUTTON_RIGHT_UP:
					case ScreenChar.BUTTON_ONE_UP:
					case ScreenChar.BUTTON_SB_UP:
					case ScreenChar.BUTTON_SB_GUIDE:
						gui.sendAidKey(tnvt.AID_ROLL_UP);
						break;

					case ScreenChar.BUTTON_LEFT_DN:
					case ScreenChar.BUTTON_MIDDLE_DN:
					case ScreenChar.BUTTON_RIGHT_DN:
					case ScreenChar.BUTTON_ONE_DN:
					case ScreenChar.BUTTON_SB_DN:
					case ScreenChar.BUTTON_SB_THUMB:

						gui.sendAidKey(tnvt.AID_ROLL_DOWN);
						break;
					case ScreenChar.BUTTON_LEFT_EB:
					case ScreenChar.BUTTON_MIDDLE_EB:
					case ScreenChar.BUTTON_RIGHT_EB:
						StringBuffer eb = new StringBuffer();
						while (planes.getWhichGUI(pos--) != ScreenChar.BUTTON_LEFT_EB)
							;
						while (planes.getWhichGUI(pos++) != ScreenChar.BUTTON_RIGHT_EB) {
							eb.append(planes.getChar(pos));
						}
						org.tn5250j.tools.system.OperatingSystem.displayURL(eb
								.toString());
						// take out the log statement when we are sure it is
						// working
						log.info("Send to external Browser: " + eb.toString());
						break;

					default:
						int aidKey = Integer.parseInt(aid.toString());
						if (aidKey >= 1 && aidKey <= 12)
							gui.sendAidKey(0x30 + aidKey);
						if (aidKey >= 13 && aidKey <= 24)
							gui.sendAidKey(0xB0 + (aidKey - 12));
					}
				} else {
					if (screenFields.getCurrentField() != null) {
						int xPos = screenFields.getCurrentField().startPos();
						for (int x = 0; x < aid.length(); x++) {
							//                  System.out.println(sr + "," + (sc + x) + " " +
							// aid.charAt(x));
							planes.setChar(xPos + x , aid.charAt(x));
						}
						//                  System.out.println(aid);
						screenFields.setCurrentFieldMDT();
						gui.sendAidKey(tnvt.AID_ENTER);
					}

				}
			} else {
				// this is a note to not execute this code here when we
				// implement
				//   the remain after edit function option.
				if (gui.rubberband.isAreaSelected()) {
					gui.rubberband.reset();
					gui.repaint();
				} else {
					goto_XY(pos);
					isInField(lastPos);
				}
			}
		}
		gui.requestFocus();
	}

	/**
	 *
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	public int getPosFromView(int x, int y) {

		// we have to translate the point into a an upper left 0,0 based format
		// to get the position into the character array which is 0,0 based.
		// we take the point of x,y and subtract the screen offsets.

		x -= bi.offLeft;
		y -= bi.offTop;

		if (x > tArea.getMaxX())
			x = (int) tArea.getMaxX() - 1;
		if (y > tArea.getMaxY())
			y = (int) tArea.getMaxY() - 1;
		if (x < tArea.getMinX())
			x = 0;
		if (y < tArea.getMinY())
			y = 0;

		//      int s = fmWidth * numCols; // image length in pixels
		//      int t = s - x; // image length minus the x point
		//      int u = t / fmWidth; //
		//      int v = numCols - u; //

		int s0 = y / fmHeight;
		int s1 = x / fmWidth;

		//      System.out.println("row " + s0 + ", column " + s1 + " pos " +
		// getPos(s0,s1));
		//      return getPos((numRows - ((((fmHeight * (numRows)) - y) /
		// fmHeight))),
		//                     (numCols - ((((fmWidth * (numCols)) - x) / fmWidth)))
		//                  );
		return getPos(s0, s1);

	}

	/**
	 * Return the row column based on the screen x,y position coordinates
	 *
	 * It will calculate a 0,0 based row and column based on the screen point
	 * coordinate.
	 *
	 * @param x
	 *            screen x position
	 * @param y
	 *            screen y position
	 *
	 * @return screen array position based 0,0 so position row 1 col 3 would be
	 *         2
	 */
	public int getRowColFromPoint(int x, int y) {

		if (x > tArea.getMaxX())
			x = (int) tArea.getMaxX() - 1;
		if (y > tArea.getMaxY())
			y = (int) tArea.getMaxY() - 1;
		if (x < tArea.getMinX())
			x = 0;
		if (y < tArea.getMinY())
			y = 0;

		//      int s = fmWidth * numCols; // image length in pixels
		//      int t = s - x; // image length minus the x point
		//      int u = t / fmWidth; //
		//      int v = numCols - u; //

		int s0 = y / fmHeight;
		int s1 = x / fmWidth;

		//      System.out.println("row " + s0 + ", column " + s1 + " pos " +
		// getPos(s0,s1));
		//      return getPos((numRows - ((((fmHeight * (numRows)) - y) /
		// fmHeight))),
		//                     (numCols - ((((fmWidth * (numCols)) - x) / fmWidth)))
		//                  );
		return getPos(s0, s1);

		//      return 0;
	}

	/**
	 * This will return the screen coordinates of a row and column.
	 *
	 * @param r
	 * @param c
	 * @param point
	 */
	public void getPointFromRowCol(int r, int c, Point point) {

		// here the x + y coordinates of the row and column are obtained from
		// the character array which is based on a upper left 0,0 coordinate
		//  we will then add to that the offsets to get the screen position point
		//  x,y coordinates. Maybe change this to a translate routine method or
		//  something.
//		point.x = screen[getPos(r, c)].x + bi.offLeft;
//		point.y = screen[getPos(r, c)].y + bi.offTop;
		point.x = (fmWidth * c) + bi.offLeft;
		point.y = (fmHeight * r) + bi.offTop;

	}

	protected void setVT(tnvt v) {

		sessionVT = v;
	}

	/**
	 * Searches the mnemonicData array looking for the specified string. If it
	 * is found it will return the value associated from the mnemonicValue
	 *
	 * @see #sendKeys
	 * @param mnem
	 *            string mnemonic value
	 * @return key value of Mnemonic
	 */
	private int getMnemonicValue(String mnem) {

		for (int x = 0; x < mnemonicData.length; x++) {

			if (mnemonicData[x].equals(mnem))
				return mnemonicValue[x];
		}
		return 0;

	}

	protected void setPrehelpState(boolean setErrorCode, boolean lockKeyboard,
			boolean unlockIfLocked) {
		statusErrorCode = setErrorCode;
		if (oia.isKeyBoardLocked() && unlockIfLocked)
			oia.setKeyBoardLocked(false);
		else
			oia.setKeyBoardLocked(lockKeyboard);
		bufferedKeys = null;
      oia.setKeysBuffered(false);


	}

	/**
	 * Activate the cursor on screen
	 *
	 * @param activate
	 */
	public void setCursorActive(boolean activate) {

		//      System.out.println("cursor active " + updateCursorLoc + " " +
		// cursorActive + " " + activate);
		if (cursorActive && !activate) {
			setCursorOff();
			cursorActive = activate;
		} else {
			if (!cursorActive && activate) {
				cursorActive = activate;
				setCursorOn();
			}
		}
	}

	/**
	 * Set the cursor on
	 */
	public void setCursorOn() {
		updateCursorLoc();
		cursorShown = false;
	}

	/**
	 * Set the cursor off
	 */
	public void setCursorOff() {

		updateCursorLoc();
		cursorShown = false;
		//      System.out.println("cursor off " + updateCursorLoc + " " +
		// cursorActive);

	}

	/**
	 *
	 */
	private void updateCursorLoc() {

		//      System.out.println("cursor loc " + updateCursorLoc + " " +
		// cursorActive);
		if (cursorActive) {

			int row = getRow(lastPos);
			int col = getCol(lastPos);

//			bi.drawCursor(this, row, col, fmWidth, fmHeight, insertMode,
//					crossHair, rulerFixed, cursorSize, colorCursor, colorBg,
//					colorWhite, font, cursorBottOffset);
			bi.drawCursor();

		}
	}

	//Added by Barry
	public String getKeys() {
		String result = this.keybuf.toString();
		this.keybuf = new StringBuffer();
		return result;
	}

	/**
	 * The sendKeys method sends a string of keys to the virtual screen. This
	 * method acts as if keystrokes were being typed from the keyboard. The
	 * keystrokes will be sent to the location given. The string being passed
	 * can also contain mnemonic values such as [enter] enter key,[tab] tab key,
	 * [pf1] pf1 etc...
	 *
	 * These will be processed as if you had pressed these keys from the
	 * keyboard. All the valid special key values are contained in the MNEMONIC
	 * enumeration:
	 *
	 * <table BORDER COLS=2 WIDTH="50%" >
	 *
	 * <tr>
	 * <td>MNEMONIC_CLEAR</td>
	 * <td>[clear]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_ENTER</td>
	 * <td>[enter]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_HELP</td>
	 * <td>[help]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PAGE_DOWN</td>
	 * <td>[pgdown]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PAGE_UP</td>
	 * <td>[pgup]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PRINT</td>
	 * <td>[print]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF1</td>
	 * <td>[pf1]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF2</td>
	 * <td>[pf2]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF3</td>
	 * <td>[pf3]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF4</td>
	 * <td>[pf4]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF5</td>
	 * <td>[pf5]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF6</td>
	 * <td>[pf6]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF7</td>
	 * <td>[pf7]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF8</td>
	 * <td>[pf8]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF9</td>
	 * <td>[pf9]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF10</td>
	 * <td>[pf10]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF11</td>
	 * <td>[pf11]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF12</td>
	 * <td>[pf12]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF13</td>
	 * <td>[pf13]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF14</td>
	 * <td>[pf14]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF15</td>
	 * <td>[pf15]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF16</td>
	 * <td>[pf16]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF17</td>
	 * <td>[pf17]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF18</td>
	 * <td>[pf18]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF19</td>
	 * <td>[pf19]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF20</td>
	 * <td>[pf20]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF21</td>
	 * <td>[pf21]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF22</td>
	 * <td>[pf22]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF23</td>
	 * <td>[pf23]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PF24</td>
	 * <td>[pf24]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_BACK_SPACE</td>
	 * <td>[backspace]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_BACK_TAB</td>
	 * <td>[backtab]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_UP</td>
	 * <td>[up]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_DOWN</td>
	 * <td>[down]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_LEFT</td>
	 * <td>[left]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_RIGHT</td>
	 * <td>[right]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_DELETE</td>
	 * <td>[delete]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_TAB</td>
	 * <td>"[tab]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_END_OF_FIELD</td>
	 * <td>[eof]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_ERASE_EOF</td>
	 * <td>[eraseeof]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_ERASE_FIELD</td>
	 * <td>[erasefld]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_INSERT</td>
	 * <td>[insert]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_HOME</td>
	 * <td>[home]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD0</td>
	 * <td>[keypad0]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD1</td>
	 * <td>[keypad1]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD2</td>
	 * <td>[keypad2]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD3</td>
	 * <td>[keypad3]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD4</td>
	 * <td>[keypad4]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD5</td>
	 * <td>[keypad5]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD6</td>
	 * <td>[keypad6]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD7</td>
	 * <td>[keypad7]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD8</td>
	 * <td>[keypad8]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD9</td>
	 * <td>[keypad9]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD_PERIOD</td>
	 * <td>[keypad.]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD_COMMA</td>
	 * <td>[keypad,]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_KEYPAD_MINUS</td>
	 * <td>[keypad-]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_FIELD_EXIT</td>
	 * <td>[fldext]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_FIELD_PLUS</td>
	 * <td>[field+]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_FIELD_MINUS</td>
	 * <td>[field-]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_BEGIN_OF_FIELD</td>
	 * <td>[bof]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PA1</td>
	 * <td>[pa1]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PA2</td>
	 * <td>[pa2]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_PA3</td>
	 * <td>[pa3]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_SYSREQ</td>
	 * <td>[sysreq]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_RESET</td>
	 * <td>[reset]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_ATTN</td>
	 * <td>[attn]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_MARK_LEFT</td>
	 * <td>[markleft]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_MARK_RIGHT</td>
	 * <td>[markright]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_MARK_UP</td>
	 * <td>[markup]</td>
	 * </tr>
	 * <tr>
	 * <td>MNEMONIC_MARK_DOWN</td>
	 * <td>[markdown]</td>
	 * </tr>
	 *
	 * </table>
	 *
	 * @param text
	 *            The string of characters to be sent
	 *
	 * @see #sendAid
	 *
	 * Added synchronized to fix a StringOutOfBounds error - Luc Gorren LDC
	 */
	public synchronized void sendKeys(String text) {

		//      if (text == null) {
		//         return;
		//      }
		this.keybuf.append(text);

		if (isStatusErrorCode() && !resetRequired) {
			setCursorActive(false);
			simulateMnemonic(getMnemonicValue("[reset]"));
			setCursorActive(true);
		}

		if (oia.isKeyBoardLocked()) {
			if (text.equals("[reset]") || text.equals("[sysreq]")
					|| text.equals("[attn]")) {
				setCursorActive(false);
				simulateMnemonic(getMnemonicValue(text));
				setCursorActive(true);

			} else {
				if (isStatusErrorCode()) {
					Toolkit.getDefaultToolkit().beep();
					return;
				}

            oia.setKeysBuffered(true);

				if (bufferedKeys == null) {
					bufferedKeys = text;
					return;
				} else {
					bufferedKeys += text;
					return;
				}
			}

		} else {

			if (oia.isKeysBuffered()) {
				if (bufferedKeys != null) {
					text = bufferedKeys + text;
				}
				//            if (text.length() == 0) {
				oia.setKeysBuffered(false);
				//            }
				bufferedKeys = null;

			}
			// check to see if position is in a field and if it is then change
			//   current field to that field
			isInField(lastPos, true);
			if (text.length() == 1 && !text.equals("[") && !text.equals("]")) {
				//               setCursorOff2();
				setCursorActive(false);
				simulateKeyStroke(text.charAt(0));
				setCursorActive(true);
				//               setCursorOn2();
				//                     System.out.println(" text one");

			} else {

				strokenizer.setKeyStrokes(text);
				String s;
				boolean done = false;

				//            setCursorOff2();
				setCursorActive(false);
				while (!done) {
					//            while (strokenizer.hasMoreKeyStrokes() && !keyboardLocked
					// &&
					//                        !isStatusErrorCode() && !done) {
					if (strokenizer.hasMoreKeyStrokes()) {

						// check to see if position is in a field and if it is
						// then change
						//   current field to that field
						isInField(lastPos, true);
						s = strokenizer.nextKeyStroke();
						if (s.length() == 1) {
							//                  setCursorOn();
							//                  if (!keysBuffered) {
							//                     System.out.println(" s two" + s);
							//                     setCursorOn();
							//                  }

							//                  try { new Thread().sleep(400);} catch
							// (InterruptedException ie) {}
							simulateKeyStroke(s.charAt(0));
							//                     System.out.println(" s two " + s + " " +
							// cursorActive);
							//                  if (cursorActive && !keysBuffered) {
							//                     System.out.println(" s two" + s);
							//                     setCursorOn();
							//                  }
						} else {

							if (s != null) {
								simulateMnemonic(getMnemonicValue(s));
								//                  if (!cursorActive && !keysBuffered) {
								//                     System.out.println(" m one");
								//                     setCursorOn();
								//                  }
							} else
								log.info(" send keys mnemonic " + s);
						}

						if (oia.isKeyBoardLocked()) {

							bufferedKeys = strokenizer
									.getUnprocessedKeyStroked();
							if (bufferedKeys != null) {
                        oia.setKeysBuffered(true);

							}
							done = true;
						}

					}

					else {
						//                  setCursorActive(true);
						//                  setCursorOn();
						done = true;
					}
				}
				setCursorActive(true);
			}
		}
	}

	/**
	 * The sendAid method sends an "aid" keystroke to the virtual screen. These
	 * aid keys can be thought of as special keystrokes, like the Enter key,
	 * PF1-24 keys or the Page Up key. All the valid special key values are
	 * contained in the AID_ enumeration:
	 *
	 * @param aidKey
	 *            The aid key to be sent to the host
	 *
	 * @see #sendKeys
	 * @see TN5250jConstants#AID_CLEAR
	 * @see #AID_ENTER
	 * @see #AID_HELP
	 * @see #AID_ROLL_UP
	 * @see #AID_ROLL_DOWN
	 * @see #AID_ROLL_LEFT
	 * @see #AID_ROLL_RIGHT
	 * @see #AID_PRINT
	 * @see #AID_PF1
	 * @see #AID_PF2
	 * @see #AID_PF3
	 * @see #AID_PF4
	 * @see #AID_PF5
	 * @see #AID_PF6
	 * @see #AID_PF7
	 * @see #AID_PF8
	 * @see #AID_PF9
	 * @see #AID_PF10
	 * @see #AID_PF11
	 * @see #AID_PF12
	 * @see #AID_PF13
	 * @see #AID_PF14
	 * @see #AID_PF15
	 * @see #AID_PF16
	 * @see #AID_PF17
	 * @see #AID_PF18
	 * @see #AID_PF19
	 * @see #AID_PF20
	 * @see #AID_PF21
	 * @see #AID_PF22
	 * @see #AID_PF23
	 * @see #AID_PF24
	 */
	public void sendAid(int aidKey) {

		sessionVT.sendAidKey(aidKey);
	}

	/**
	 * Restores the error line and sets the error mode off.
	 *
	 */
	public void resetError() {

		restoreErrorLine();
		setStatus(STATUS_ERROR_CODE, STATUS_VALUE_OFF, "");

	}

	protected boolean simulateMnemonic(int mnem) {

		boolean simulated = false;

		switch (mnem) {

		case AID_CLEAR:
		case AID_ENTER:
		case AID_PF1:
		case AID_PF2:
		case AID_PF3:
		case AID_PF4:
		case AID_PF5:
		case AID_PF6:
		case AID_PF7:
		case AID_PF8:
		case AID_PF9:
		case AID_PF10:
		case AID_PF11:
		case AID_PF12:
		case AID_PF13:
		case AID_PF14:
		case AID_PF15:
		case AID_PF16:
		case AID_PF17:
		case AID_PF18:
		case AID_PF19:
		case AID_PF20:
		case AID_PF21:
		case AID_PF22:
		case AID_PF23:
		case AID_PF24:
		case AID_ROLL_DOWN:
		case AID_ROLL_UP:
		case AID_ROLL_LEFT:
		case AID_ROLL_RIGHT:

			if (!screenFields.isCanSendAid()) {
				displayError(ERR_ENTER_NO_ALLOWED);
			} else
				sendAid(mnem);
			simulated = true;

			break;
		case AID_HELP:
			sessionVT.sendHelpRequest();
			simulated = true;
			break;

		case AID_PRINT:
			sessionVT.hostPrint(1);
			simulated = true;
			break;

		case BACK_SPACE:
			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {

				if (screenFields.getCurrentField().startPos() == lastPos)
					displayError(ERR_CURSOR_PROTECTED);

				else {
					screenFields.getCurrentField().getKeyPos(lastPos);
					screenFields.getCurrentField().changePos(-1);
					resetDirty(screenFields.getCurrentField().getCurrentPos());
					shiftLeft(screenFields.getCurrentField().getCurrentPos());
					updateDirty();
					screenFields.setCurrentFieldMDT();

					simulated = true;
				}
			} else {
				displayError(ERR_CURSOR_PROTECTED);

			}
			break;
		case BACK_TAB:

			if (screenFields.getCurrentField() != null
					&& screenFields.isCurrentFieldHighlightedEntry()) {
				resetDirty(screenFields.getCurrentField().startPos);
				gotoFieldPrev();
				updateDirty();
			} else
				gotoFieldPrev();

			if (screenFields.isCurrentFieldContinued()) {
				do {
					gotoFieldPrev();
				} while (screenFields.isCurrentFieldContinuedMiddle()
						|| screenFields.isCurrentFieldContinuedLast());
			}
			isInField(lastPos);
			simulated = true;
			break;
		case UP:
		case MARK_UP:
			process_XY(lastPos - numCols);
			simulated = true;
			break;
		case DOWN:
		case MARK_DOWN:
			process_XY(lastPos + numCols);
			simulated = true;
			break;
		case LEFT:
		case MARK_LEFT:

			process_XY(lastPos - 1);
			simulated = true;
			break;
		case RIGHT:
		case MARK_RIGHT:
			process_XY(lastPos + 1);
			simulated = true;
			break;
		case NEXTWORD:
			gotoNextWord();
			simulated = true;
			break;
		case PREVWORD:
			gotoPrevWord();
			simulated = true;
			break;
		case DELETE:
			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {

				resetDirty(lastPos);
				screenFields.getCurrentField().getKeyPos(lastPos);
				shiftLeft(screenFields.getCurrentFieldPos());
				screenFields.setCurrentFieldMDT();
				updateDirty();
				simulated = true;
			} else {
				displayError(ERR_CURSOR_PROTECTED);
			}

			break;
		case TAB:

			if (screenFields.getCurrentField() != null
					&& !screenFields.isCurrentFieldContinued()) {
				if (screenFields.isCurrentFieldHighlightedEntry()) {
					resetDirty(screenFields.getCurrentField().startPos);
					gotoFieldNext();
					updateDirty();
				} else
					gotoFieldNext();
			} else {
				do {
					gotoFieldNext();
				} while (screenFields.getCurrentField() != null
						&& (screenFields.isCurrentFieldContinuedMiddle() || screenFields
								.isCurrentFieldContinuedLast()));
			}

			isInField(lastPos);
			simulated = true;

			break;
		case EOF:
			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {
				int where = endOfField(screenFields.getCurrentField()
						.startPos(), true);
				if (where > 0) {
					goto_XY((where / numCols) + 1, (where % numCols) + 1);
				}
				simulated = true;
			} else {
				displayError(ERR_CURSOR_PROTECTED);
			}
			resetDirty(lastPos);

			break;
		case ERASE_EOF:
			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {

				int where = lastPos;
				resetDirty(lastPos);
				if (fieldExit()) {
					screenFields.setCurrentFieldMDT();
					if (!screenFields.isCurrentFieldContinued()) {
						gotoFieldNext();
					} else {
						do {
							gotoFieldNext();
							if (screenFields.isCurrentFieldContinued())
								fieldExit();
						} while (screenFields.isCurrentFieldContinuedMiddle()
								|| screenFields.isCurrentFieldContinuedLast());
					}
				}
				updateDirty();
				goto_XY(where);
				simulated = true;

			} else {
				displayError(ERR_CURSOR_PROTECTED);
			}

			break;
		case ERASE_FIELD:
			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {

				int where = lastPos;
				lastPos = screenFields.getCurrentField().startPos();
				resetDirty(lastPos);
				if (fieldExit()) {
					screenFields.setCurrentFieldMDT();
					if (!screenFields.isCurrentFieldContinued()) {
						gotoFieldNext();
					} else {
						do {
							gotoFieldNext();
							if (screenFields.isCurrentFieldContinued())
								fieldExit();
						} while (screenFields.isCurrentFieldContinuedMiddle()
								|| screenFields.isCurrentFieldContinuedLast());
					}
				}
				updateDirty();
				goto_XY(where);
				simulated = true;

			} else {
				displayError(ERR_CURSOR_PROTECTED);
			}

			break;
		case INSERT:
			// we toggle it
			insertMode = insertMode ? false : true;
			break;
		case HOME:
			// position to the home position set
			if (lastPos + numCols + 1 != homePos) {
				goto_XY(homePos - numCols - 1);
				// now check if we are in a field
				isInField(lastPos);
			} else
				gotoField(1);
			break;
		case KEYPAD_0:
			simulated = simulateKeyStroke('0');
			break;
		case KEYPAD_1:
			simulated = simulateKeyStroke('1');
			break;
		case KEYPAD_2:
			simulated = simulateKeyStroke('2');
			break;
		case KEYPAD_3:
			simulated = simulateKeyStroke('3');
			break;
		case KEYPAD_4:
			simulated = simulateKeyStroke('4');
			break;
		case KEYPAD_5:
			simulated = simulateKeyStroke('5');
			break;
		case KEYPAD_6:
			simulated = simulateKeyStroke('6');
			break;
		case KEYPAD_7:
			simulated = simulateKeyStroke('7');
			break;
		case KEYPAD_8:
			simulated = simulateKeyStroke('8');
			break;
		case KEYPAD_9:
			simulated = simulateKeyStroke('9');
			break;
		case KEYPAD_PERIOD:
			simulated = simulateKeyStroke('.');
			break;
		case KEYPAD_COMMA:
			simulated = simulateKeyStroke(',');
			break;
		case KEYPAD_MINUS:
			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {

				int s = screenFields.getCurrentField().getFieldShift();
				if (s == 3 || s == 5 || s == 7) {
//					screen[lastPos].setChar('-');
               planes.setChar(lastPos,'-');

					resetDirty(lastPos);
					advancePos();
					if (fieldExit()) {
						screenFields.setCurrentFieldMDT();
						if (!screenFields.isCurrentFieldContinued()) {
							gotoFieldNext();
						} else {
							do {
								gotoFieldNext();
							} while (screenFields
									.isCurrentFieldContinuedMiddle()
									|| screenFields
											.isCurrentFieldContinuedLast());
						}
						simulated = true;
						updateDirty();
						if (screenFields.isCurrentFieldAutoEnter())
							sendAid(AID_ENTER);

					}
				} else {
					displayError(ERR_FIELD_MINUS);

				}
			} else {
				displayError(ERR_CURSOR_PROTECTED);
			}

			break;
		case FIELD_EXIT:
			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {

				resetDirty(lastPos);

				if (fieldExit()) {
					screenFields.setCurrentFieldMDT();
					if (!screenFields.isCurrentFieldContinued()) {
						gotoFieldNext();
					} else {
						do {
							gotoFieldNext();
							if (screenFields.isCurrentFieldContinued())
								fieldExit();
						} while (screenFields.isCurrentFieldContinuedMiddle()
								|| screenFields.isCurrentFieldContinuedLast());
					}
				}

				updateDirty();
				simulated = true;
				if (screenFields.isCurrentFieldAutoEnter())
					sendAid(AID_ENTER);

			} else {
				displayError(ERR_CURSOR_PROTECTED);
			}

			break;
		case FIELD_PLUS:
			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {

				resetDirty(lastPos);
				if (fieldExit()) {
					screenFields.setCurrentFieldMDT();
					if (!screenFields.isCurrentFieldContinued()) {
						gotoFieldNext();
					} else {
						do {
							gotoFieldNext();
						} while (screenFields.isCurrentFieldContinuedMiddle()
								|| screenFields.isCurrentFieldContinuedLast());
					}
				}
				updateDirty();
				simulated = true;
				if (screenFields.isCurrentFieldAutoEnter())
					sendAid(AID_ENTER);

			} else {
				displayError(ERR_CURSOR_PROTECTED);
			}

			break;
		case FIELD_MINUS:
			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {

				int s = screenFields.getCurrentField().getFieldShift();
				if (s == 3 || s == 5 || s == 7) {
//					screen[lastPos].setChar('-');
               planes.setChar(lastPos,'-');

					resetDirty(lastPos);
					advancePos();
					if (fieldExit()) {
						screenFields.setCurrentFieldMDT();
						if (!screenFields.isCurrentFieldContinued()) {
							gotoFieldNext();
						} else {
							do {
								gotoFieldNext();
							} while (screenFields
									.isCurrentFieldContinuedMiddle()
									|| screenFields
											.isCurrentFieldContinuedLast());
						}
					}
					updateDirty();
					simulated = true;
					if (screenFields.isCurrentFieldAutoEnter())
						sendAid(AID_ENTER);

				} else {
					displayError(ERR_FIELD_MINUS);

				}
			} else {
				displayError(ERR_CURSOR_PROTECTED);
			}

			break;
		case BOF:
			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {
				int where = screenFields.getCurrentField().startPos();
				if (where > 0) {
					goto_XY(where);
				}
				simulated = true;
			} else {
				displayError(ERR_CURSOR_PROTECTED);
			}
			resetDirty(lastPos);

			break;
		case SYSREQ:
			sessionVT.systemRequest();
			simulated = true;
			break;
		case RESET:
			if (isStatusErrorCode()) {
				resetError();
				isInField(lastPos);
				updateDirty();
			} else {
				setPrehelpState(false, oia.isKeyBoardLocked(), false);
			}
			gui.repaint();
			simulated = true;
			break;
		case COPY:
			copyMe();
			break;
		case PASTE:
			pasteMe(false);
			break;
		case ATTN:
			sessionVT.sendAttentionKey();
			simulated = true;
			break;
		case DUP_FIELD:
			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {

				if (screenFields.isCurrentFieldDupEnabled()) {
					resetDirty(lastPos);
					screenFields.getCurrentField().setFieldChar(lastPos,
							(char) 0x1C);
					screenFields.setCurrentFieldMDT();
					gotoFieldNext();
					updateDirty();
					simulated = true;
				} else {
					displayError(ERR_DUP_KEY_NOT_ALLOWED);
				}
			} else {
				displayError(ERR_CURSOR_PROTECTED);
			}

			break;
		case NEW_LINE:
			if (screenFields.getSize() > 0) {
				int startRow = getRow(lastPos) + 1;
				int startPos = lastPos;
				boolean isthere = false;

				if (startRow == getRows())
					startRow = 0;

				goto_XY(++startRow, 1);

				if (!isInField() && screenFields.getCurrentField() != null
						&& !screenFields.isCurrentFieldBypassField()) {
					while (!isInField()
							&& screenFields.getCurrentField() != null
							&& !screenFields.isCurrentFieldBypassField()) {

						// lets keep going
						advancePos();

						// Have we looped the screen?
						if (lastPos == startPos) {
							// if so then go back to starting point
							goto_XY(startPos);
							break;
						}
					}
				}
			}
			simulated = true;
			break;
		case JUMP_NEXT_SESS:
			gui.nextSession();
			simulated = true;
			break;
		case JUMP_PREV_SESS:
			gui.prevSession();
			simulated = true;
			break;
		default:
			log.info(" Mnemonic not supported " + mnem);
			break;

		}

		return simulated;
	}

	protected boolean simulateKeyStroke(char c) {

		//      if (isStatusErrorCode() && !Character.isISOControl(c) &&
		// !keyProcessed) {
		//         if (resetRequired)
		//            return false;
		//         else
		//            resetError();
		//      }

		boolean updateField = false;
		boolean numericError = false;
		boolean updatePos = false;
		boolean autoEnter = false;

		if (!Character.isISOControl(c)) {

			if (screenFields.getCurrentField() != null
					&& screenFields.withinCurrentField(lastPos)
					&& !screenFields.isCurrentFieldBypassField()) {

				if (screenFields.isCurrentFieldFER()
						&& !screenFields.withinCurrentField(screenFields
								.getCurrentFieldPos())
						&& lastPos == screenFields.getCurrentField().endPos()
						&& screenFields.getCurrentFieldPos() > screenFields
								.getCurrentField().endPos()) {

					displayError(ERR_FIELD_EXIT_INVALID);
					feError = true;
					return false;
				}

				switch (screenFields.getCurrentFieldShift()) {
				case 0: // Alpha shift
				case 2: // Numeric Shift
				case 4: // Kakana Shift
					updateField = true;
					break;
				case 1: // Alpha Only
					if (Character.isLetter(c) || c == ',' || c == '-'
							|| c == '.' || c == ' ')
						updateField = true;
					break;
				case 3: // Numeric only
					if (Character.isDigit(c) || c == '+' || c == ','
							|| c == '-' || c == '.' || c == ' ')
						updateField = true;
					else
						numericError = true;
					break;
				case 5: // Digits only
					if (Character.isDigit(c))
						updateField = true;
					else
						displayError(ERR_NUMERIC_09);
					break;
				case 7: // Signed numeric
					if (Character.isDigit(c) || c == '+' || c == '-')
						if (lastPos == screenFields.getCurrentField().endPos()
								&& (c != '+' && c != '-'))
							displayError(ERR_INVALID_SIGN);
						else
							updateField = true;
					else
						displayError(ERR_NUMERIC_09);
					break;
				}

				if (updateField) {
					if (screenFields.isCurrentFieldToUpper())
						c = Character.toUpperCase(c);

					updatePos = true;
					resetDirty(lastPos);

					if (insertMode) {
						if (endOfField(false) != screenFields.getCurrentField()
								.endPos())
							shiftRight(lastPos);
						else {

							displayError(ERR_NO_ROOM_INSERT);
							updatePos = false;
						}

					}

					if (updatePos) {
						screenFields.getCurrentField().getKeyPos(
								getRow(lastPos), getCol(lastPos));
						screenFields.getCurrentField().changePos(1);

//						screen[lastPos].setChar(c);
                  planes.setChar(lastPos,c);

						screenFields.setCurrentFieldMDT();

						// if we have gone passed the end of the field then goto
						// the next field
						if (!screenFields.withinCurrentField(screenFields
								.getCurrentFieldPos())) {
							if (screenFields.isCurrentFieldAutoEnter()) {
								autoEnter = true;
							} else if (!screenFields.isCurrentFieldFER())
								gotoFieldNext();
							else {
								//                        screenFields.getCurrentField().changePos(1);
								//
								//                        if (screenFields.
								//                        cursorPos == endPos)
								//                           System.out.println("end of field");
								//
								//                        feError != feError;
								//                        if (feError)
								//                           displayError(ERR_FIELD_EXIT_INVALID);
							}

						} else
							goto_XY(screenFields.getCurrentField()
									.getCursorRow() + 1, screenFields
									.getCurrentField().getCursorCol() + 1);

					}

					updateImage(dirty);

					if (autoEnter)
						sendAid(AID_ENTER);
				} else {
					if (numericError) {
						displayError(ERR_NUMERIC_ONLY);
					}
				}
			} else {
				displayError(ERR_CURSOR_PROTECTED);
			}

		}
		return updatePos;
	}

	/**
	 * @todo: Change to be mnemonic key.
	 *
	 * This toggles the ruler line.
	 *
	 *
	 */
	public void crossHair() {
		setCursorActive(false);
		crossHair++;
		if (crossHair > 3)
			crossHair = 0;
		setCursorActive(true);
	}

	/**
	 * Method: endOfField
	 * <p>
	 *
	 * convenience method that call endOfField with lastRow lastCol and passes
	 * the posSpace to that method
	 *
	 * @param posSpace
	 *            value of type boolean - specifying to return the position of
	 *            the the last space or not
	 * @return a value of type int - the screen postion (row * columns) + col
	 *
	 */
	private int endOfField(boolean posSpace) {
		return endOfField(lastPos, posSpace);
	}

	/**
	 * Method: endOfField
	 * <p>
	 *
	 * gets the position of the last character of the current field posSpace
	 * parameter tells the routine whether to return the position of the last
	 * space ( <= ' ') or the last non space posSpace == true last occurrence of
	 * char <= ' ' posSpace == false last occurrence of char > ' '
	 *
	 * @param pos
	 *            value of type int - position to start from
	 * @param posSpace
	 *            value of type boolean - specifying to return the position of
	 *            the the last space or not
	 * @return a value of type int - the screen postion (row * columns) + col
	 *
	 */
	private int endOfField(int pos, boolean posSpace) {

		int endPos = screenFields.getCurrentField().endPos();
		int fePos = endPos;
		// get the number of characters to the right
		int count = endPos - pos;

		// first lets get the real ending point without spaces and the such
//		while (screen[endPos].getChar() <= ' ' && count-- > 0) {
		while (planes.getChar(endPos) <= ' ' && count-- > 0) {

			endPos--;
		}

		if (endPos == fePos) {

			return endPos;

		} else {
			screenFields.getCurrentField().getKeyPos(endPos);
			if (posSpace)
				screenFields.getCurrentField().changePos(+1);

			return screenFields.getCurrentFieldPos();

		}
	}

	private boolean fieldExit() {

		int pos = lastPos;
		boolean mdt = false;
		int end = endOfField(false); // get the ending position of the first
		// non blank character in field

		ScreenField sf = screenFields.getCurrentField();

		if (sf.isMandatoryEnter() && end == sf.startPos()) {
			displayError(ERR_MANDITORY_ENTER);
			return false;
		}

		// get the number of characters to the right
		int count = (end - sf.startPos()) - sf.getKeyPos(pos);

		if (count == 0 && sf.isFER()) {
			mdt = true;
			return mdt;
		}

		for (; count >= 0; count--) {
//			screen[pos].setChar(initChar);
         planes.setChar(pos,initChar);
			setDirty(pos);
			pos++;
			mdt = true;
		}

		int adj = sf.getAdjustment();

		if (adj != 0) {

			switch (adj) {

			case 5:
				//               System.out.println("Right adjust, zero fill " +
				// screenFields.getCurrentField().getAdjustment());
				rightAdjustField('0');
				sf.setRightAdjusted();
				break;
			case 6:
				//               System.out.println("Right adjust, blank fill " +
				// screenFields.getCurrentField().getAdjustment());
				rightAdjustField(' ');
				sf.setRightAdjusted();

				break;
			case 7:
				System.out.println("Mandatory fill "
						+ screenFields.getCurrentField().getAdjustment());
				sf.setManditoryEntered();
				break;

			}
		} else {

			// we need to right adjust signed numeric fields as well.
			if (sf.isSignedNumeric()) {
				rightAdjustField(' ');
			}
		}

		return mdt;
	}

	private void rightAdjustField(char fill) {

		int end = endOfField(false); // get the ending position of the first
		// non blank character in field

		// get the number of characters to the right
		int count = screenFields.getCurrentField().endPos() - end;

		// subtract 1 from count for signed numeric - note for later
		if (screenFields.getCurrentField().isSignedNumeric()) {
//			if (screen[end - 1].getChar() != '-')
			if (planes.getChar(end -1) != '-')
				count--;
		}

		int pos = screenFields.getCurrentField().startPos();

		while (count-- >= 0) {

			shiftRight(pos);
//			screen[pos].setChar(fill);
         planes.setChar(pos,fill);

			setDirty(pos);

		}

	}

	private void shiftLeft(int sPos) {

		int endPos = 0;

		int pos = sPos;
		int pPos = sPos;

		ScreenField sf = screenFields.getCurrentField();
		int end;
		int count;
		do {
			end = endOfField(pPos, false); // get the ending position of the
										   // first
			// non blank character in field

			count = (end - screenFields.getCurrentField().startPos())
					- screenFields.getCurrentField().getKeyPos(pPos);

			// now we loop through and shift the remaining characters to the
			// left
			while (count-- > 0) {
				pos++;
//				screen[pPos].setChar(screen[pos].getChar());
            planes.setChar(pPos,planes.getChar(pos));
				setDirty(pPos);
				pPos = pos;

			}

			if (screenFields.isCurrentFieldContinued()) {
				gotoFieldNext();
				if (screenFields.getCurrentField().isContinuedFirst())
					break;

				pos = screenFields.getCurrentField().startPos();
//				screen[pPos].setChar(screen[pos].getChar());
            planes.setChar(pPos,planes.getChar(pos));
				setDirty(pPos);

				pPos = pos;

			}
		} while (screenFields.isCurrentFieldContinued()
				&& !screenFields.getCurrentField().isContinuedFirst());

		if (end >= 0 && count >= -1) {

			endPos = end;
		} else {
			endPos = sPos;

		}

		screenFields.setCurrentField(sf);
//		screen[endPos].setChar(initChar);
      planes.setChar(endPos,initChar);
		setDirty(endPos);
		goto_XY(screenFields.getCurrentFieldPos());
		sf = null;

	}

	private void shiftRight(int sPos) {

		int end = endOfField(true); // get the ending position of the first
		// non blank character in field
		int pos = end;
		int pPos = end;

		int count = end - sPos;

		// now we loop through and shift the remaining characters to the right
		while (count-- > 0) {

			pos--;
//			screen[pPos].setChar(screen[pos].getChar());
			planes.setChar(pPos, planes.getChar(pos));
			setDirty(pPos);

			pPos = pos;
		}
	}

	public int getRow(int pos) {

		//      if (pos == 0)
		//         return 1;

		int row = pos / numCols;

		if (row < 0) {

			row = lastPos / numCols;
		}
		if (row > lenScreen - 1)
			row = lenScreen - 1;

		return row;

	}

	public int getCol(int pos) {

		//      if (pos == 0)
		//         return 1;

		int col = pos % (getCols());
		if (col > 0)
			return col;
		else
			return 0;
	}

	/**
	 * This routine is 0 based offset. So to get row 20,1 then pass row 19,0
	 *
	 * @param row
	 * @param col
	 * @return
	 */
	public int getPos(int row, int col) {

		return (row * numCols) + col;
	}

	/**
	 * Current position is based on offsets of 1,1 not 0,0 of the current
	 * position of the screen
	 *
	 * @return int
	 */
	public int getCurrentPos() {

		return lastPos + numCols + 1;

	}

	/**
	 * I got this information from a tcp trace of each error. I could not find
	 * any documenation for this. Maybe there is but I could not find it. If
	 * anybody finds this documention could you please send me a copy. Please
	 * note that I did not look that hard either.
	 *
	 * <p>
	 * 0000: 00 50 73 1D 89 81 00 50 DA 44 C8 45 08 00 45 00 .Ps....P.D.E..E.
	 * </p>
	 * <p>
	 * 0010: 00 36 E9 1C 40 00 80 06 9B F9 C1 A8 33 58 C0 A8 .6..@.......3X..
	 * </p>
	 * <p>
	 * 0020: C0 02 06 0E 00 17 00 52 6E 88 73 40 DE CB 50 18 .......Rn.s@..P.
	 * </p>
	 * <p>
	 * 0030: 20 12 3C 53 00 00 00 0C 12 A0 00 00 04 01 00 00 . <S............
	 * </p>
	 * <p>
	 * 0040: 00 05 FF EF .... ----------|| The 00 XX is the code to be sent. I
	 * found the following
	 *
	 * <table BORDER COLS=2 WIDTH="50%" >
	 *
	 * <tr>
	 * <td>ERR_CURSOR_PROTECTED</td>
	 * <td>0x05</td>
	 * </tr>
	 * <tr>
	 * <td>ERR_INVALID_SIGN</td>
	 * <td>0x11</td>
	 * </tr>
	 * <tr>
	 * <td>ERR_NO_ROOM_INSERT</td>
	 * <td>0x12</td>
	 * </tr>
	 * <tr>
	 * <td>ERR_NUMERIC_ONLY</td>
	 * <td>0x09</td>
	 * </tr>
	 * <tr>
	 * <td>ERR_NUMERIC_09</td>
	 * <td>0x10</td>
	 * </tr>
	 * <tr>
	 * <td>ERR_FIELD_MINUS</td>
	 * <td>0x16</td>
	 * </tr>
	 * <tr>
	 * <td>ERR_ENTER_NOT_ALLOWED</td>
	 * <td>0x20</td>
	 * </tr>
	 * <tr>
	 * <td>ERR_MANDITORY_ENTER</td>
	 * <td>0x21</td>
	 * </tr>
	 * <tr>
	 * <td>ERR_ENTER_NOT_ALLOWED</td>
	 * <td>0x20</td>
	 * </tr>
	 *
	 * </table> I am tired of typing and they should be self explanitory.
	 * Finding them in the first place was the pain.
	 * </p>
	 *
	 * @param ec
	 *            error code
	 *
	 */
	private void displayError(int ec) {
		saveHomePos = homePos;
		homePos = lastPos + numCols + 1;
		pendingInsert = true;
		gui.sendNegResponse2(ec);

	}

	private void process_XY(int pos) {

		if (pos < 0)
			pos = lenScreen + pos;
		if (pos > lenScreen - 1)
			pos = pos - lenScreen;

		// if there was a field exit error then we need to treat the movement
		//  of the cursor in a special way that equals that of Client Access.
		//    If the cursor is moved from the field then we need to reset the
		//       position within the field so that the last character can be typed
		//       over again instead of sending the field exit error again.
		//       We also need to reset the field exit error flag.
		//
		//    How we know we have a field exit error is when the field position is
		//    set beyond the end of the field and a character is then typed we can
		//    not position that character. To reset this we need to set the next
		//    position of the field to not be beyond the end of field but to the
		//    last character.
		//
		//    Now to make it work like Client Access if the cursor is a back space
		//    then do not move the cursor but place it on the last field. All
		//    other keys will reset the field position so that entering over the
		//    last character will not cause an error but replace that character or
		//    just plain move the cursor if the key was to do that.

		if (feError) {
			feError = false;
			screenFields.getCurrentField().changePos(-1);

			if (screenFields.getCurrentField() != null
					&& screenFields.getCurrentField().isFER()
					&& screenFields.getCurrentFieldPos() - 1 == pos) {
			}
		} else {
			goto_XY(pos);
		}
	}

	public boolean isUsingGuiInterface() {

		return guiInterface;
	}

	/**
	 * Convinience class to return if the cursor is in a field or not.
	 *
	 * @return true or false
	 */

	public boolean isInField() {

		return isInField(lastPos, true);
	}

	/**
	 *
	 * Convinience class to return if the position that is passed is in a field
	 * or not. If it is then the chgToField parameter will change the current
	 * field to this field where the position indicates
	 *
	 * @param pos
	 * @param chgToField
	 * @return true or false
	 */
	public boolean isInField(int pos, boolean chgToField) {

		return screenFields.isInField(pos, chgToField);
	}

	/**
	 *
	 * Convinience class to return if the position that is passed is in a field
	 * or not. If it is then the field at this position becomes the current
	 * working field
	 *
	 * @param pos
	 * @return true or false
	 */
	public boolean isInField(int pos) {

		return screenFields.isInField(pos, true);
	}

	/**
	 * Convinience class to return if the position at row and column that is
	 * passed is in a field or not. If it is then the field at this position
	 * becomes the current working field.
	 *
	 * @param row
	 * @param col
	 * @return true or false
	 */
	public boolean isInField(int row, int col) {

		return isInField(row, col, true);
	}

	/**
	 *
	 * Convinience class to return if the position at row and column that is
	 * passed is in a field or not. If it is then the chgToField parameter will
	 * change the current field to this field where the row and column
	 * indicates.
	 *
	 * @param row
	 * @param col
	 * @param chgToField
	 * @return true or false
	 */
	public boolean isInField(int row, int col, boolean chgToField) {
		return screenFields.isInField((row * numCols) + col, chgToField);
	}

	/**
	 * Gets the length of the screen - number of rows times number of columns
	 *
	 * @return int value of screen length
	 */
	public int getScreenLength() {

		return lenScreen;
	}

	/**
	 * Get the number or rows available.
	 *
	 * @return number of rows
	 */
	public int getRows() {

		return numRows;

	}

	/**
	 * Get the number of columns available.
	 *
	 * @return number of columns
	 */
	public int getCols() {

		return numCols;

	}

	/**
	 * Get the current row where the cursor is
	 *
	 * @return the cursor current row position 1,1 based
	 */
	public int getCurrentRow() {

		return (lastPos / numCols) + 1;

	}

	/**
	 * Get the current column where the cursor is
	 *
	 * @return the cursor current column position 1,1 based
	 */
	public int getCurrentCol() {

		return (lastPos % numCols) + 1;

	}

	/**
	 * The last position of the cursor on the screen - Note - position is based
	 * 0,0
	 *
	 * @return last position
	 */
	protected int getLastPos() {

		return lastPos;

	}

	/**
	 * Hotspot More... string
	 *
	 * @return string literal of More...
	 */
	public StringBuffer getHSMore() {
		return hsMore;
	}

	/**
	 * Hotspot Bottom string
	 *
	 * @return string literal of Bottom
	 */
	public StringBuffer getHSBottom() {
		return hsBottom;
	}

	/**
	 * The column separator to be used
	 *
	 * @return column separator to be used values: 0 - line 1 - dot 2 - short
	 *         line 3 - do not show column separator
	 */
	public int getColSepLine() {
		return colSepLine;
	}

	/**
	 * Should the screen attributes be show in hex
	 *
	 * @return true we should and false we should not
	 */
	public boolean isShowHex() {
		return showHex;
	}

	/**
	 * Return the whole screen represented as a character array
	 *
	 * @return character array containing the text
	 *
	 * Added by Luc - LDC
	 *
	 * Note to KJP - Have to ask what the difference is between this method and
	 * the other
	 */
	public char[] getScreenAsAllChars() {
		char[] sac = new char[lenScreen];
		char c;

		for (int x = 0; x < lenScreen; x++) {
//			c = screen[x].getChar();
         c = planes.getChar(x);
			// only draw printable characters (in this case >= ' ')
//			if ((c >= ' ') && (!screen[x].isAttributePlace())) {
			if ((c >= ' ') && (!planes.isAttributePlace(x))) {
				sac[x] = c;
            // TODO: implement the underline check here
//				if (screen[x].underLine && c <= ' ')
//					sac[x] = '_';
			} else
				sac[x] = ' ';
		}

		return sac;
	}

	/**
	 *
	 * Return the screen represented as a character array
	 *
	 * @return character array containing the text
	 */
	public char[] getScreenAsChars() {
		char[] sac = new char[lenScreen];
		char c;

		for (int x = 0; x < lenScreen; x++) {
//			c = screen[x].getChar();
         c = planes.getChar(x);
			// only draw printable characters (in this case >= ' ')
//			if ((c >= ' ') && (!screen[x].isAttributePlace())) {
			if ((c >= ' ') && (!planes.isAttributePlace(x))) {
				sac[x] = c;
            // TODO: implement the underline check here
//				if (screen[x].underLine && c <= ' ')
//					sac[x] = '_';
			} else
				sac[x] = ' ';
		}

		return sac;
	}

   /**
    * <p>
    *  GetScreen retrieves the various planes associated with the presentation
    *  space. The data is returned as a linear array of character values in the
    *  array provided. The array is not terminated by a null character except
    *  when data is retrieved from the text plane, in which case a single null
    *  character is appended.
    *  </p>
    *  <p>
    *  The application must supply a buffer for the returned data and the length
    *  of the buffer. Data is returned starting from the beginning of the
    *  presentation space and continuing until the buffer is full or the entire
    *  plane has been copied. For text plane data, the buffer must include one
    *  extra position for the terminating null character.
    *  <p>
    *
    * @param buffer
    * @param bufferLength
    * @param plane
    * @return The number of characters copied to the buffer
    * @throws OhioException
    */
   public synchronized int GetScreen(char buffer[], int bufferLength, int plane)
//                                       throws OhioException {
                                       {
      return GetScreen(buffer,bufferLength,0,lenScreen,plane);

   }

   /**
    * <p>
    *  GetScreen retrieves the various planes associated with the presentation
    *  space. The data is returned as a linear array of character values in the
    *  array provided. The array is not terminated by a null character except
    *  when data is retrieved from the text plane, in which case a single null
    *  character is appended.
    * </p>
    * <p>
    * The application must supply a buffer for the returned data and the length
    * of the buffer. Data is returned starting from the given position and
    * continuing until the specified number of characters have been copied, the
    * buffer is full or the entire plane has been copied. For text plane data,
    * the buffer must include one extra position for the terminating null character.
    * </p>
    *
    * @param buffer
    * @param bufferLength
    * @param from
    * @param length
    * @param plane
    * @return The number of characters copied to the buffer
    * @throws OhioException
    */
   public synchronized int GetScreen(char buffer[], int bufferLength, int from,
                                    int length, int plane)
//                                    throws OhioException {
                                    {

      return planes.GetScreen(buffer,bufferLength, from, length, plane);
   }

   /**
    * <p>
    *  GetScreen retrieves the various planes associated with the presentation
    *  space. The data is returned as a linear array of character values in the
    *  array provided. The array is not terminated by a null character except
    *  when data is retrieved from the text plane, in which case a single null
    *  character is appended.
    *  </p>
    *  <p>
    *  The application must supply a buffer for the returned data and the length
    *  of the buffer. Data is returned starting from the given coordinates and
    *  continuing until the specified number of characters have been copied,
    *  the buffer is full, or the entire plane has been copied. For text plane
    *  data, the buffer must include one extra position for the terminating null
    *  character.
    *  </p>
    *
    * @param buffer
    * @param bufferLength
    * @param row
    * @param col
    * @param length
    * @param plane
    * @return The number of characters copied to the buffer.
    * @throws OhioException
    */
   public synchronized int GetScreen(char buffer[], int bufferLength, int row,
                                       int col, int length, int plane)
//                                       throws OhioException {
                                       {
      // Call GetScreen function after converting row and column to
      // a position.
      return planes.GetScreen(buffer,bufferLength, row, col, length, plane);
   }

   /**
    * <p>
    *  GetScreenRect retrieves data from the various planes associated with the
    *  presentation space. The data is returned as a linear array of character
    *  values in the buffer provided.
    *  </p>
    *
    * <p>
    * The application supplies two positions that represent opposing corners of
    * a rectangle within the presentation space. The starting and ending
    * positions can have any spatial relationship to each other. The data
    * returned starts from the row containing the upper-most point to the row
    * containing the lower-most point, and from the left-most column to the
    * right-most column.
    * </p>
    * <p>
    * The specified buffer must be at least large enough to contain the number
    * of characters in the rectangle. If the buffer is too small, no data is
    * copied and zero is returned by the method. Otherwise, the method returns
    * the number of characters copied.
    * </p>
    *
    * @param buffer
    * @param bufferLength
    * @param startPos
    * @param endPos
    * @param plane
    * @return The number of characters copied to the buffer
    * @throws OhioException
    */
   public synchronized int GetScreenRect(char buffer[], int bufferLength,
                                             int startPos, int endPos, int plane)
//                                             throws OhioException {
                                             {
      return planes.GetScreenRect(buffer, bufferLength, startPos, endPos, plane);

   }

   /**
    * <p>
    *  GetScreenRect retrieves data from the various planes associated with the
    *  presentation space. The data is returned as a linear array of character
    *  values in the buffer provided. The buffer is not terminated by a null
    *  character.
    * </p>
    * <p>
    * The application supplies two coordinates that represent opposing corners
    * of a rectangle within the presentation space. The starting and ending
    * coordinates can have any spatial relationship to each other. The data
    * returned starts from the row containing the upper-most point to the row
    * containing the lower-most point, and from the left-most column to the
    * right-most column.
    * </p>
    * <p>
    * The specified buffer must be at least large enough to contain the number
    * of characters in the rectangle. If the buffer is too small, no data is
    * copied and zero is returned by the method. Otherwise, the method returns
    * the number of characters copied.
    * </p>
    *
    * @param buffer
    * @param bufferLength
    * @param startRow
    * @param startCol
    * @param endRow
    * @param endCol
    * @param plane
    * @return The number characters copied to the buffer
    * @throws OhioException
    */
   public synchronized int GetScreenRect(char buffer[], int bufferLength,
                                             int startRow, int startCol,
                                             int endRow, int endCol, int plane)
//                                             throws OhioException {
                                             {

      return planes.GetScreenRect(buffer, bufferLength, startRow, startCol, endRow,
                                    endCol, plane);
   }

	/**
	 * This routine is based on offset 1,1 not 0,0 it will translate to offset
	 * 0,0 and call the goto_XY(int pos) it is mostly used from external classes
	 * that use the 1,1 offset
	 *
	 * @param row
	 * @param col
	 */
	public void goto_XY(int row, int col) {
		goto_XY(((row - 1) * numCols) + (col - 1));
	}

	// this routine is based on offset 0,0 not 1,1
	public void goto_XY(int pos) {
		//      setCursorOff();
		updateCursorLoc();
		lastPos = pos;
		//      setCursorOn();
		updateCursorLoc();
	}

	/**
	 * This returns whether or not any of the fields currently on the screen
	 * have been changed in any way.
	 *
	 * Convinience class to ScreenFields
	 *
	 * @return true or false
	 * @see org#tn5250j#ScreenFields
	 */
	public boolean isMasterMDT() {

		return screenFields.isMasterMDT();

	}

	/**
	 * Set the current working field to the field number specified.
	 *
	 * @param f -
	 *            numeric field number on the screen
	 * @return true or false whether it was sucessful
	 */
	public boolean gotoField(int f) {

		int sizeFields = screenFields.getSize();

		if (f > sizeFields || f <= 0)
			return false;

		screenFields.setCurrentField(screenFields.getField(f - 1));

		while (screenFields.isCurrentFieldBypassField() && f < sizeFields) {

			screenFields.setCurrentField(screenFields.getField(f++));

		}
		return gotoField(screenFields.getCurrentField());
	}

	/**
	 * Convenience method to set the field object passed as the currect working
	 * screen field
	 *
	 * @param f
	 * @return true or false whether it was sucessful
	 * @see org.tn5250j.ScreenField
	 */
	protected boolean gotoField(ScreenField f) {
		if (f != null) {
			goto_XY(f.startPos());
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Convenience class to position the cursor to the next word on the screen
	 *
	 */
	/**
	 * Convenience class to position the cursor to the next word on the screen
	 *
	 */
	private void gotoNextWord() {

		int pos = lastPos;

//		if (screen[lastPos].getChar() > ' ') {
		if (planes.getChar(lastPos) > ' ') {
			advancePos();
			// get the next space character
//			while (screen[lastPos].getChar() > ' ' && pos != lastPos) {
			while (planes.getChar(lastPos) > ' ' && pos != lastPos) {
				advancePos();
			}
		} else
			advancePos();

		// now that we are positioned on the next space character get the
		// next none space character
//		while (screen[lastPos].getChar() <= ' ' && pos != lastPos) {
		while (planes.getChar(lastPos) <= ' ' && pos != lastPos) {
			advancePos();
		}

	}

	/**
	 * Convenience class to position the cursor to the previous word on the
	 * screen
	 *
	 */
	private void gotoPrevWord() {

		int pos = lastPos;

		changePos(-1);

		// position previous white space character
//		while (screen[lastPos].getChar() <= ' ') {
		while (planes.getChar(lastPos) <= ' ') {
			changePos(-1);
			if (pos == lastPos)
				break;
		}

		changePos(-1);
		// get the previous space character
//		while (screen[lastPos].getChar() > ' ' && pos != lastPos) {
		while (planes.getChar(lastPos) > ' ' && pos != lastPos) {
			changePos(-1);
		}

		// and position one position more should give us the beginning of word
		advancePos();

	}

	/**
	 * Convinience class to position to the next field on the screen.
	 *
	 * @see org.tn5250j.ScreenFields
	 */
	private void gotoFieldNext() {

		if (screenFields.isCurrentFieldHighlightedEntry())
			unsetFieldHighlighted(screenFields.getCurrentField());

		screenFields.gotoFieldNext();

		if (screenFields.isCurrentFieldHighlightedEntry())
			setFieldHighlighted(screenFields.getCurrentField());
	}

	/**
	 * Convinience class to position to the previous field on the screen.
	 *
	 * @see org.tn5250j.ScreenFields
	 */
	private void gotoFieldPrev() {

		if (screenFields.isCurrentFieldHighlightedEntry())
			unsetFieldHighlighted(screenFields.getCurrentField());

		screenFields.gotoFieldPrev();

		if (screenFields.isCurrentFieldHighlightedEntry())
			setFieldHighlighted(screenFields.getCurrentField());

	}

	/**
	 * Used to restrict the cursor to a particular position on the screen. Used
	 * in combination with windows to restrict the cursor to the active window
	 * show on the screen.
	 *
	 * Not supported yet. Please implement me :-(
	 *
	 * @param depth
	 * @param width
	 */
	public void setRestrictCursor(int depth, int width) {

		restrictCursor = true;
		//      restriction

	}

	/**
	 * Creates a window on the screen
	 *
	 * @param depth
	 * @param width
	 * @param type
	 * @param gui
	 * @param monoAttr
	 * @param colorAttr
	 * @param ul
	 * @param upper
	 * @param ur
	 * @param left
	 * @param right
	 * @param ll
	 * @param bottom
	 * @param lr
	 */
	public void createWindow(int depth, int width, int type, boolean gui,
			int monoAttr, int colorAttr, int ul, int upper, int ur, int left,
			int right, int ll, int bottom, int lr) {

		int c = getCol(lastPos);
		int w = 0;
		width++;

		w = width;
		// set leading attribute byte
//		screen[lastPos].setCharAndAttr(initChar, initAttr, true);
		planes.setScreenCharAndAttr(lastPos, initChar, initAttr, true);
		setDirty(lastPos);

		advancePos();
		// set upper left
//		screen[lastPos].setCharAndAttr((char) ul, colorAttr, false);
		planes.setScreenCharAndAttr(lastPos, (char) ul, colorAttr, false);
		if (gui) {
//			screen[lastPos].setUseGUI(ScreenChar.UPPER_LEFT);
			planes.setUseGUI(lastPos, ScreenChar.UPPER_LEFT);
		}
		setDirty(lastPos);

		advancePos();

		// draw top row

		while (w-- >= 0) {
//			screen[lastPos].setCharAndAttr((char) upper, colorAttr, false);
			planes.setScreenCharAndAttr(lastPos, (char) upper, colorAttr, false);
			if (gui) {
//				screen[lastPos].setUseGUI(ScreenChar.UPPER);
				planes.setUseGUI(lastPos,ScreenChar.UPPER);
			}
			setDirty(lastPos);
			advancePos();
		}

		// set upper right
//		screen[lastPos].setCharAndAttr((char) ur, colorAttr, false);
		planes.setScreenCharAndAttr(lastPos,(char) ur, colorAttr, false);

		if (gui) {
//			screen[lastPos].setUseGUI(ScreenChar.UPPER_RIGHT);
			planes.setUseGUI(lastPos, ScreenChar.UPPER_RIGHT);
		}
		setDirty(lastPos);
		advancePos();

		// set ending attribute byte
//		screen[lastPos].setCharAndAttr(initChar, initAttr, true);
		planes.setScreenCharAndAttr(lastPos,initChar, initAttr, true);

		setDirty(lastPos);

		lastPos = ((getRow(lastPos) + 1) * numCols) + c;
		// now handle body of window
		while (depth-- > 0) {

			// set leading attribute byte
//			screen[lastPos].setCharAndAttr(initChar, initAttr, true);
			planes.setScreenCharAndAttr(lastPos,initChar, initAttr, true);
			setDirty(lastPos);
			advancePos();

			// set left
//			screen[lastPos].setCharAndAttr((char) left, colorAttr, false);
			planes.setScreenCharAndAttr(lastPos, (char) left, colorAttr, false);

			if (gui) {
//				screen[lastPos].setUseGUI(ScreenChar.LEFT);
				planes.setUseGUI(lastPos,ScreenChar.LEFT);
			}
			setDirty(lastPos);
			advancePos();

			w = width;
			// fill it in
			while (w-- >= 0) {
//				screen[lastPos].setCharAndAttr(initChar, initAttr, true);
				planes.setScreenCharAndAttr(lastPos,initChar, initAttr, true);
//				screen[lastPos].setUseGUI(ScreenChar.NO_GUI);
				planes.setUseGUI(lastPos,ScreenChar.NO_GUI);
				setDirty(lastPos);
				advancePos();
			}

			// set right
//			screen[lastPos].setCharAndAttr((char) right, colorAttr, false);
			planes.setScreenCharAndAttr(lastPos,(char) right, colorAttr, false);
			if (gui) {
//				screen[lastPos].setUseGUI(ScreenChar.RIGHT);
				planes.setUseGUI(lastPos,ScreenChar.RIGHT);
			}
			setDirty(lastPos);
			advancePos();

			// set ending attribute byte
//			screen[lastPos].setCharAndAttr(initChar, initAttr, true);
			planes.setScreenCharAndAttr(lastPos,initChar, initAttr, true);
			setDirty(lastPos);

			lastPos = ((getRow(lastPos) + 1) * numCols) + c;
		}

		// set leading attribute byte
//		screen[lastPos].setCharAndAttr(initChar, initAttr, true);
		planes.setScreenCharAndAttr(lastPos,initChar, initAttr, true);
		setDirty(lastPos);
		advancePos();

		// set lower left
//		screen[lastPos].setCharAndAttr((char) ll, colorAttr, false);
		planes.setScreenCharAndAttr(lastPos,(char) ll, colorAttr, false);
		if (gui) {
//			screen[lastPos].setUseGUI(ScreenChar.LOWER_LEFT);
			planes.setUseGUI(lastPos,ScreenChar.LOWER_LEFT);
		}
		setDirty(lastPos);
		advancePos();

		w = width;
		// draw bottom row
		while (w-- >= 0) {
//			screen[lastPos].setCharAndAttr((char) bottom, colorAttr, false);
			planes.setScreenCharAndAttr(lastPos,(char) bottom, colorAttr, false);
			if (gui) {
//				screen[lastPos].setUseGUI(ScreenChar.BOTTOM);
				planes.setUseGUI(lastPos,ScreenChar.BOTTOM);
			}
			setDirty(lastPos);
			advancePos();
		}

		// set lower right
//		screen[lastPos].setCharAndAttr((char) lr, colorAttr, false);
		planes.setScreenCharAndAttr(lastPos, (char) lr, colorAttr, false);
		if (gui) {
//			screen[lastPos].setUseGUI(ScreenChar.LOWER_RIGHT);
			planes.setUseGUI(lastPos,ScreenChar.LOWER_RIGHT);
		}
		setDirty(lastPos);
		advancePos();

		// set ending attribute byte
//		screen[lastPos].setCharAndAttr(initChar, initAttr, true);
		planes.setScreenCharAndAttr(lastPos,initChar, initAttr, true);
		setDirty(lastPos);

	}

	/**
	 * Creates a scroll bar on the screen using the parameters provided.
	 *  ** we only support vertical scroll bars at the time.
	 *
	 * @param flag -
	 *            type to draw - vertical or horizontal
	 * @param totalRowScrollable
	 * @param totalColScrollable
	 * @param sliderRowPos
	 * @param sliderColPos
	 * @param sbSize
	 */
	public void createScrollBar(int flag, int totalRowScrollable,
			int totalColScrollable, int sliderRowPos, int sliderColPos,
			int sbSize) {

		//      System.out.println("Scrollbar flag: " + flag +
		//                           " scrollable Rows: " + totalRowScrollable +
		//                           " scrollable Cols: " + totalColScrollable +
		//                           " thumb Row: " + sliderRowPos +
		//                           " thumb Col: " + sliderColPos +
		//                           " size: " + sbSize +
		//                           " row: " + getRow(lastPos) +
		//                           " col: " + getCol(lastPos));

		int sp = lastPos;
		int size = sbSize - 2;

		int thumbPos = (int) (size * (float) ((float) sliderColPos / (float) totalColScrollable));
		//      System.out.println(thumbPos);
//		screen[sp].setCharAndAttr(' ', 32, false);
//		screen[sp].setUseGUI(ScreenChar.BUTTON_SB_UP);
		planes.setScreenCharAndAttr(sp,' ', 32, false);
		planes.setUseGUI(sp,ScreenChar.BUTTON_SB_UP);

		int ctr = 0;
		while (ctr < size) {
			sp += numCols;
//			screen[sp].setCharAndAttr(' ', 32, false);
			planes.setScreenCharAndAttr(sp,' ', 32, false);
			if (ctr == thumbPos)
//				screen[sp].setUseGUI(ScreenChar.BUTTON_SB_THUMB);
				planes.setUseGUI(sp,ScreenChar.BUTTON_SB_THUMB);
			else
//				screen[sp].setUseGUI(ScreenChar.BUTTON_SB_GUIDE);
				planes.setUseGUI(sp, ScreenChar.BUTTON_SB_GUIDE);
			ctr++;
		}
		sp += numCols;

//		screen[sp].setCharAndAttr(' ', 32, false);
//		screen[sp].setUseGUI(ScreenChar.BUTTON_SB_DN);

		planes.setScreenCharAndAttr(sp, ' ', 32, false);
		planes.setUseGUI(sp, ScreenChar.BUTTON_SB_DN);
	}

	/**
	 * Write the title of the window that is on the screen
	 *
	 * @param pos
	 * @param depth
	 * @param width
	 * @param orientation
	 * @param monoAttr
	 * @param colorAttr
	 * @param title
	 */
	public void writeWindowTitle(int pos, int depth, int width,
			byte orientation, int monoAttr, int colorAttr, StringBuffer title) {

		int sp = lastPos;
		int len = title.length();

		// get bit 0 and 1 for interrogation
		switch (orientation & 0xc0) {
		case 0x40: // right
			pos += (4 + width - len);
			break;
		case 0x80: // left
			pos += 2;
			break;
		default: // center
			// this is to place the position to the first text position of the
			// window
			//    the position passed in is the first attribute position, the next
			//    is the border character and then there is another attribute after
			//    that.
			pos += (3 + ((width / 2) - (len / 2)));
			break;

		}

		//  if bit 2 is on then this is a footer
		if ((orientation & 0x20) == 0x20)
			pos += ((depth + 1) * numCols);

		//      System.out.println(pos + "," + width + "," + len+ "," + getRow(pos)
		//                              + "," + getCol(pos) + "," + ((orientation >> 6) & 0xf0));

		for (int x = 0; x < len; x++) {
//			screen[pos].setChar(title.charAt(x));
//			screen[pos++].setUseGUI(ScreenChar.NO_GUI);
			planes.setChar(pos, title.charAt(x));
			planes.setUseGUI(pos++, ScreenChar.NO_GUI);

		}
	}

	/**
	 * Roll the screen up or down.
	 *
	 * Byte 1: Bit 0 0 = Roll up 1 = Roll down Bits 1-2 Reserved Bits 3-7 Number
	 * of lines that the designated area is to be rolled Byte 2: Bits 0-7 Line
	 * number defining the top line of the area that will participate in the
	 * roll. Byte 3: Bits 0-7 Line number defining the bottom line of the area
	 * that will participate in the roll.
	 *
	 * @param direction
	 * @param topLine
	 * @param bottomLine
	 */
	public void rollScreen(int direction, int topLine, int bottomLine) {

		// get the number of lines which are the last 5 bits
		int lines = direction & 0x7F;
		// get the direction of the roll which is the first bit
		//    0 - up
		//    1 - down
		int updown = direction & 0x80;

		// calculate the reference points for the move.
		int start = this.getPos(topLine - 1, 0);
		int end = this.getPos(bottomLine - 1, numCols - 1);
		int len = end - start;

		//      System.out.println(" starting roll");
		//      dumpScreen();
		switch (updown) {
		case 0:
			//  Now round em up and head em UP.
			for (int x = start; x < len + numCols; x++) {
//				screen[x].setChar(screen[x + numCols].getChar());
				planes.setChar(x, planes.getChar(x + numCols));
			}
			break;
		case 1:
			//  Now round em up and head em DOWN.
			for (int x = end + numCols; x > 0; x--) {
//				screen[x + numCols].setChar(screen[x].getChar());
				planes.setChar(x + numCols, planes.getChar(x));
			}
			break;
		default:
			log.warn(" Invalid roll parameter - please report this");
		}
		//      System.out.println(" end roll");
		//      dumpScreen();

	}

	public void dumpScreen() {

		StringBuffer sb = new StringBuffer();
		char[] s = getScreenAsChars();
		int c = getCols();
		int l = getRows() * c;
		int col = 0;
		for (int x = 0; x < l; x++, col++) {
			sb.append(s[x]);
			if (col == c) {
				sb.append('\n');
				col = 0;
			}
		}
		System.out.println(sb.toString());

	}

	/**
	 * Add a field to the field format table.
	 *
	 * @param attr -
	 *            Field attribute
	 * @param len -
	 *            length of field
	 * @param ffw1 -
	 *            Field format word 1
	 * @param ffw2 -
	 *            Field format word 2
	 * @param fcw1 -
	 *            Field control word 1
	 * @param fcw2 -
	 *            Field control word 2
	 */
	public void addField(int attr, int len, int ffw1, int ffw2, int fcw1,
			int fcw2) {

		lastAttr = attr;

//		screen[lastPos].setCharAndAttr(initChar, lastAttr, true);
		planes.setScreenCharAndAttr(lastPos, initChar, lastAttr, true);

		setDirty(lastPos);

		advancePos();

		ScreenField sf = null;

		// from 14.6.12 for Start of Field Order 5940 function manual
		//  examine the format table for an entry that begins at the current
		//  starting address plus 1.
		if (screenFields.existsAtPos(lastPos)) {
			screenFields.setCurrentFieldFFWs(ffw1, ffw2);
		} else {
			sf = screenFields.setField(attr, getRow(lastPos), getCol(lastPos),
					len, ffw1, ffw2, fcw1, fcw2);
			lastPos = sf.startPos();
			int x = len;

			boolean gui = guiInterface;
			if (sf.isBypassField())
				gui = false;

			while (x-- > 0) {

//				if (screen[lastPos].getChar() == 0)
//					screen[lastPos].setCharAndAttr(' ', lastAttr, false);
//				else
//					screen[lastPos].setAttribute(lastAttr);

				if (planes.getChar(lastPos) == 0)
					planes.setScreenCharAndAttr(lastPos, ' ', lastAttr, false);
				else
					planes.setScreenAttr(lastPos,lastAttr);

				if (gui) {
//					screen[lastPos].setUseGUI(ScreenChar.FIELD_MIDDLE);
					planes.setUseGUI(lastPos,ScreenChar.FIELD_MIDDLE);
				}

				advancePos();

			}

			if (gui)
				if (len > 1) {
//					screen[sf.startPos()].setUseGUI(ScreenChar.FIELD_LEFT);
					planes.setUseGUI(sf.startPos(), ScreenChar.FIELD_LEFT);
//					if (lastPos > 0)
//						screen[lastPos - 1].setUseGUI(ScreenChar.FIELD_RIGHT);
//					else
//						screen[lastPos].setUseGUI(ScreenChar.FIELD_RIGHT);

					if (lastPos > 0)
						planes.setUseGUI(lastPos - 1, ScreenChar.FIELD_RIGHT);
					else
						planes.setUseGUI(lastPos,ScreenChar.FIELD_RIGHT);

				}
            else {
//					screen[lastPos - 1].setUseGUI(ScreenChar.FIELD_ONE);
					planes.setUseGUI(lastPos - 1,ScreenChar.FIELD_ONE);
            }

			//         screen[lastPos].setCharAndAttr(initChar,initAttr,true);
			setEndingAttr(initAttr);

			lastPos = sf.startPos();
		}

		//      if (fcw1 != 0 || fcw2 != 0) {

		//         System.out.println("lr = " + lastRow + " lc = " + lastCol + " " +
		// sf.toString());
		//      }
		sf = null;

	}

	class ChoiceField {

		int x;

		int y;

		int width;

		int height;

		char mnemonic;

		int fieldId;

	}

	Vector choices;

	public void addChoiceField(String text) {

		if (choices == null) {
			choices = new Vector(3);
		}

		ChoiceField cf = new ChoiceField();
		cf.fieldId = screenFields.getCurrentField().getFieldId();
		choices.add(cf);

	}

	//      public void addChoiceField(int attr, int len, int ffw1, int ffw2, int
	// fcw1, int fcw2) {
	//
	//         lastAttr = attr;
	//
	//         screen[lastPos].setCharAndAttr(initChar,lastAttr,true);
	//         setDirty(lastPos);
	//
	//         advancePos();
	//
	//         boolean found = false;
	//         ScreenField sf = null;
	//
	//         // from 14.6.12 for Start of Field Order 5940 function manual
	//         // examine the format table for an entry that begins at the current
	//         // starting address plus 1.
	//         for (int x = 0;x < sizeFields; x++) {
	//            sf = screenFields[x];
	//
	//            if (lastPos == sf.startPos()) {
	//               screenFields.getCurrentField() = sf;
	//               screenFields.getCurrentField().setFFWs(ffw1,ffw2);
	//               found = true;
	//            }
	//
	//         }
	//
	//         if (!found) {
	//            sf =
	// setField(attr,getRow(lastPos),getCol(lastPos),len,ffw1,ffw2,fcw1,fcw2);
	//
	//            lastPos = sf.startPos();
	//            int x = len;
	//
	//            boolean gui = guiInterface;
	//            if (sf.isBypassField())
	//               gui = false;
	//
	//            while (x-- > 0) {
	//
	//               if (screen[lastPos].getChar() == 0)
	//                  screen[lastPos].setCharAndAttr(' ',lastAttr,false);
	//               else
	//                  screen[lastPos].setAttribute(lastAttr);
	//
	//               if (gui)
	//                  screen[lastPos].setUseGUI(ScreenChar.FIELD_MIDDLE);
	//
	//               advancePos();
	//
	//            }
	//
	//            if (gui)
	//               if (len > 1) {
	//                  screen[sf.startPos()].setUseGUI(ScreenChar.FIELD_LEFT);
	//                  if (lastPos > 0)
	//                     screen[lastPos-1].setUseGUI(ScreenChar.FIELD_RIGHT);
	//                  else
	//                     screen[lastPos].setUseGUI(ScreenChar.FIELD_RIGHT);
	//
	//               }
	//               else
	//                  screen[lastPos-1].setUseGUI(ScreenChar.FIELD_ONE);
	//
	//            setEndingAttr(initAttr);
	//
	//            lastPos = sf.startPos();
	//         }
	//
	//   // if (fcw1 != 0 || fcw2 != 0) {
	//   //
	//   // System.out.println("lr = " + lastRow + " lc = " + lastCol + " " +
	// sf.toString());
	//   // }
	//         sf = null;
	//
	//      }

	/**
	 * Return the fields that are contained in the Field Format Table
	 *
	 * @return ScreenFields object
	 * @see org.tn5250j.ScreenFields
	 */
	public ScreenFields getScreenFields() {
		return screenFields;
	}

	/**
	 * Redraw the fields on the screen. Used for gui enhancement to redraw the
	 * fields when toggling
	 *
	 */
	public void drawFields() {

		ScreenField sf;

		int sizeFields = screenFields.getSize();
		for (int x = 0; x < sizeFields; x++) {

			sf = screenFields.getField(x);

			if (!sf.isBypassField()) {
				int pos = sf.startPos();

				int l = sf.length;

				boolean f = true;

				if (l >= lenScreen)
					l = lenScreen - 1;

				if (l > 1) {
					while (l-- > 0) {

						if (guiInterface && f) {
//							screen[pos].setUseGUI(ScreenChar.FIELD_LEFT);
							planes.setUseGUI(pos,ScreenChar.FIELD_LEFT);
							f = false;
						} else {

//							screen[pos].setUseGUI(ScreenChar.FIELD_MIDDLE);
							planes.setUseGUI(pos,ScreenChar.FIELD_MIDDLE);

						}

						if (guiInterface && l == 0) {
//							screen[pos].setUseGUI(ScreenChar.FIELD_RIGHT);
							planes.setUseGUI(pos,ScreenChar.FIELD_RIGHT);
						}

						pos++;
					}
				} else {
//					screen[pos].setUseGUI(ScreenChar.FIELD_ONE);
					planes.setUseGUI(pos,ScreenChar.FIELD_ONE);
				}
			}
		}

	}

	/**
	 * Draws the field on the screen. Used to redraw or change the attributes of
	 * the field.
	 *
	 * @param sf -
	 *            Field to be redrawn
	 * @see org.tn5250j.ScreenField.java
	 */
	public void drawField(ScreenField sf) {

		int pos = sf.startPos();

		int x = sf.length;

		while (x-- > 0) {
			setDirty(pos++);
		}
		//      updateImage(dirty);

	}

	/**
	 * Set the field to be displayed as highlighted.
	 *
	 * @param sf -
	 *            Field to be highlighted
	 */
	public void setFieldHighlighted(ScreenField sf) {

		int pos = sf.startPos();

		int x = sf.length;
		int na = sf.getHighlightedAttr();

		while (x-- > 0) {
//			screen[pos].setAttribute(na);
			planes.setScreenAttr(pos,na);
			setDirty(pos++);
		}
		updateImage(dirty);

	}

	/**
	 * Draw the field as un higlighted. This is used to reset the field
	 * presentation on the screen after the field is exited.
	 *
	 * @param sf -
	 *            Field to be unhighlighted
	 */
	public void unsetFieldHighlighted(ScreenField sf) {

		int pos = sf.startPos();

		int x = sf.length;
		int na = sf.getAttr();

		while (x-- > 0) {
//			screen[pos].setAttribute(na);
			planes.setScreenAttr(pos,na);
			setDirty(pos++);
		}
		updateImage(dirty);

	}

	public boolean checkHotSpots() {

//		boolean retHS = false;
      // needs to be re implemented
//		retHS = GUIHotSpots.checkHotSpots(this, screen, numRows, numCols,
//				lenScreen, fmWidth, fmHeight);

//		return retHS;
	   return planes.checkHotSpots();
	}

	public void setChar(int cByte) {

		if (cByte > 0 && cByte < ' ') {
//			screen[lastPos].setCharAndAttr((char) 0x00, 33, false);
			planes.setScreenCharAndAttr(lastPos, (char) 0x00, 33, false);
			setDirty(lastPos);

			advancePos();
		} else {
			if (lastPos > 0) {

            ////  change me here once things are working
//				if (screen[lastPos - 1].isAttributePlace())// &&
//					lastAttr = screen[lastPos - 1].getCharAttr();
            // uncomment me
				if (planes.isAttributePlace(lastPos - 1)) // &&
					lastAttr = planes.getCharAttr(lastPos - 1);
			}

//			screen[lastPos].setCharAndAttr((char) cByte, lastAttr, false);
			planes.setScreenCharAndAttr(lastPos, (char) cByte, lastAttr, false);

			//         screen[lastPos].setCharAndAttr((char)cByte,
			//                           screen[lastPos].getCharAttr(),false);

			setDirty(lastPos);
			if (guiInterface && !isInField(lastPos, false)) {
//				screen[lastPos].setUseGUI(ScreenChar.NO_GUI);
				planes.setUseGUI(lastPos, ScreenChar.NO_GUI);
			}
			advancePos();
		}

	}

	public void setEndingAttr(int cByte) {

		int attr = lastAttr;
		//      System.out.println("setting ending to " + cByte + " lastAttr is " +
		// lastAttr +
		//                     " at " + (lastRow + 1) + "," + (lastCol + 1));
		//      System.out.print("setting ending to ");

		setAttr(cByte);
		lastAttr = attr;
	}

	public void setAttr(int cByte) {
		lastAttr = cByte;

		//      int sattr = screen[lastPos].getCharAttr();
		//         System.out.println("changing from " + sattr + " to attr " + lastAttr
		// +
		//                     " at " + (this.getRow(lastPos) + 1) + "," + (this.getCol(lastPos) +
		// 1));
//		screen[lastPos].setCharAndAttr(initChar, lastAttr, true);
		planes.setScreenCharAndAttr(lastPos, initChar, lastAttr, true);
		setDirty(lastPos);

		advancePos();
		int pos = lastPos;

		int times = 0;
		//      sattr = screen[lastPos].getCharAttr();
		//         System.out.println(" next position after change " + sattr + " last
		// attr " + lastAttr +
		//                     " at " + (this.getRow(lastPos) + 1) + "," + (this.getCol(lastPos) +
		// 1) +
		//                     " attr place " + screen[lastPos].isAttributePlace());

      ///  un - comment me here;
//		while (screen[lastPos].getCharAttr() != lastAttr
//				&& !screen[lastPos].isAttributePlace()) {

		while (planes.getCharAttr(lastPos) != lastAttr
				&& !planes.isAttributePlace(lastPos)) {

//			screen[lastPos].setAttribute(lastAttr);
			planes.setScreenAttr(lastPos, lastAttr);
			if (guiInterface && !isInField(lastPos, false)) {
//				int g = screen[lastPos].whichGui;
//				if (g >= ScreenChar.FIELD_LEFT && g <= ScreenChar.FIELD_ONE)
//					screen[lastPos].setUseGUI(ScreenChar.NO_GUI);
				int g = planes.getWhichGUI(lastPos);
				if (g >= ScreenChar.FIELD_LEFT && g <= ScreenChar.FIELD_ONE)
					planes.setUseGUI(lastPos,ScreenChar.NO_GUI);
			}
			setDirty(lastPos);

			times++;
			advancePos();
		}

		// sanity check for right now
		//      if (times > 200)
		//         System.out.println(" setAttr = " + times + " start = " + (sr + 1) +
		// "," + (sc + 1));

		lastPos = pos;
	}

	/**
	 * Draw or redraw the dirty parts of the screen and display them.
	 *
	 * Rectangle dirty holds the dirty area of the screen to be updated.
	 *
	 * If you want to change the screen in anyway you need to set the screen
	 * attributes before calling this routine.
	 */
	public void updateDirty() {

		//LDC - 12/02/2003 - check if we must repaint it
		if (drawing == false)
			return;
		//LDC - 18/02/2003 - only update the screen when the window is showing
		//if (this.gui.isShowing())
		//{
		Rectangle r = new Rectangle(dirty);

		// update the image
		updateImage(r);
		//}
		// update dirty to show that we have already painted that region of the
		//   screen so do not do it again.
		//      int height = (int)(tArea.getHeight() - dirty.height);
		//      if (height > 0)
		//      dirty.setBounds(dirty.x,dirty.height,dirty.width,(int)(tArea.getHeight()
		// - dirty.height));
		//LDC - 12/02/2003 - set drawing to false
		drawing = false;

	}

	public void setDirty(int pos) {

//		int bx = screen[pos].x;
//		int by = screen[pos].y;
      int bx = fmWidth * getCol(pos);
      int by = fmHeight * getRow(pos);
		//LDC - 12/02/2003 - if we must drawing something, do a union with it
		//  otherwise only this rectangle must be redrawing
		if (drawing) {
			workR.setBounds(bx, by, fmWidth, fmHeight);
			dirty = dirty.union(workR);
		} else {
			dirty.setBounds(bx, by, fmWidth, fmHeight);
			drawing = true;
		}

	}

	private void setDirty(int row, int col) {

		setDirty(getPos(row, col));

	}

	private void resetDirty(int pos) {

//      int x = s.fmWidth * col;
//      int y = s.fmHeight * row;
      int x = fmWidth * getCol(pos);
      int y = fmHeight * this.getRow(pos);

//		dirty.setBounds(screen[pos].x, screen[pos].y, fmWidth, fmHeight);
		dirty.setBounds(x, y, fmWidth, fmHeight);
		//LDC - 12/02/2003 - It must be painting this area
		drawing = true;
	}

	/**
	 * Change the screen position by one column
	 */
	protected void advancePos() {
		changePos(1);
	}

	/**
	 * Change position of the screen by the increment of parameter passed.
	 *
	 * If the position change is under the minimum of the first screen position
	 * then the position is moved to the last row and column of the screen.
	 *
	 * If the position change is over the last row and column of the screen then
	 * cursor is moved to first position of the screen.
	 *
	 * @param i
	 */
	protected void changePos(int i) {

		lastPos += i;
		if (lastPos < 0)
			lastPos = lenScreen + lastPos;
		if (lastPos > lenScreen - 1)
			lastPos = lastPos - lenScreen;

		//      System.out.println(lastRow + "," + ((lastPos) / numCols) + "," +
		//                         lastCol + "," + ((lastPos) % numCols) + "," +
		//                         ((lastRow * numCols) + lastCol) + "," +
		//                         (lastPos));

	}

	public void goHome() {

		//  now we try to move to first input field according to
		//  14.6 WRITE TO DISPLAY Command
		//    ? If the WTD command is valid, after the command is processed,
		//          the cursor moves to one of three locations:
		//    - The location set by an insert cursor order (unless control
		//          character byte 1, bit 1 is equal to B'1'.)
		//    - The start of the first non-bypass input field defined in the
		//          format table
		//    - A default starting address of row 1 column 1.

		if (pendingInsert && homePos > 0) {
			goto_XY(getRow(homePos), getCol(homePos));
			isInField(); // we now check if we are in a field
		} else {
			if (!gotoField(1)) {
				homePos = getPos(1, 1);
				goto_XY(1, 1);
				isInField(0, 0); // we now check if we are in a field
			} else {
				homePos = getPos(getCurrentRow(), getCurrentCol());
			}
		}
	}

	public void setPendingInsert(boolean flag, int icX, int icY) {
		pendingInsert = flag;
		if (pendingInsert) {
			homePos = getPos(icX, icY);
		}

		if (!isStatusErrorCode()) {
			goto_XY(icX, icY);
		}
	}

	public void setPendingInsert(boolean flag) {
		if (homePos != -1)
			pendingInsert = flag;
	}

	/**
	 * Set the error line number to that of number passed.
	 *
	 * @param line
	 */
	public void setErrorLine(int line) {

		if (line == 0 || line > numRows)
			errorLineNum = numRows;
		else
			errorLineNum = line;
	}

	/**
	 * Returns the current error line number
	 *
	 * @return current error line number
	 */
	public int getErrorLine() {
		return errorLineNum;
	}

	/**
	 * Saves off the current error line characters to be used later.
	 *
	 */
	public void saveErrorLine() {
//		// if there is already an error line saved then do not save it again
//		//  This signifies that there was a previous error and the original error
//		//  line was not restored yet.
//		if (errorLine == null) {
//			errorLine = new ScreenChar[numCols];
//			int r = getPos(errorLineNum - 1, 0);
//
//			for (int x = 0; x < numCols; x++) {
//				errorLine[x] = new ScreenChar(this);
//				errorLine[x].setCharAndAttr(screen[r + x].getChar(), screen[r
//						+ x].getCharAttr(), false);
//			}
//		}
      planes.saveErrorLine();
	}

	/**
	 * Restores the error line characters from the save buffer.
	 *
	 * @see #saveErrorLine()
	 */
	public void restoreErrorLine() {

		if (planes.isErrorLineSaved()) {
         planes.restoreErrorLine();
         int y = fmHeight * planes.getErrorLine();
			updateImage(0, y, bi.getWidth(), fmHeight);
		}
	}

	public void setStatus(byte attr, byte value, String s) {

		// set the status area
		switch (attr) {

		case STATUS_SYSTEM:
			if (value == STATUS_VALUE_ON) {
            oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,ScreenOIA.OIA_LEVEL_INPUT_INHIBITED, s);
				statusXSystem = true;
			}
         else {
				statusXSystem = false;
            oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,ScreenOIA.OIA_LEVEL_NOT_INHIBITED,s);
         }
			break;

		case STATUS_ERROR_CODE:
			if (value == STATUS_VALUE_ON) {
				setPrehelpState(true, true, false);
				oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
				      						ScreenOIA.OIA_LEVEL_INPUT_ERROR,s);

				Toolkit.getDefaultToolkit().beep();
			} else {
			   oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
			         						ScreenOIA.OIA_LEVEL_NOT_INHIBITED);
				setPrehelpState(false, true, true);
				homePos = saveHomePos;
				saveHomePos = 0;
				pendingInsert = false;
			}
			break;

		}
	}

	public boolean isWithinScreenArea(int x, int y) {

		//      if (x == 0 || y == 0)
		//         return true;
		//
		//      x -= bi.offLeft;
		//      y -= bi.offTop;

		return tArea.contains(x, y);

	}

	protected boolean isStatusErrorCode() {

		return oia.getLevel() == ScreenOIA.OIA_LEVEL_INPUT_ERROR;

	}

	/**
	 * This routine clears the screen, resets row and column to 0, resets the
	 * last attribute to 32, clears the fields, turns insert mode off,
	 * clears/initializes the screen character array.
	 */
	public void clearAll() {

		lastAttr = 32;
		lastPos = 0;

		clearTable();
		clearScreen();
//		screen[0].setAttribute(initAttr);
      planes.setScreenAttr(0,initAttr);
		insertMode = false;
		cursor.setRect(0, 0, 0, 0);
      oia.setInsertMode(insertMode);
	}

	/**
	 * Clear the fields table
	 */
	public void clearTable() {

		oia.setKeyBoardLocked(true);
		screenFields.clearFFT();
		pendingInsert = false;
		homePos = -1;
	}

	/**
	 * Clear the gui constructs
	 *
	 */
	protected void clearGuiStuff() {

		for (int x = 0; x < lenScreen; x++) {
			//         screen[x].setCharAndAttr(' ',initAttr,false);
//			screen[x].setUseGUI(ScreenChar.NO_GUI);
         planes.setUseGUI(x,ScreenChar.NO_GUI);
		}
		dirty.setBounds(tArea.getBounds());
		//      dirty.setBounds(fmWidth * numCols,fmHeight * numRows,0,0);
	}

	/**
	 * Clear the screen by setting the initial character and initial attribute
	 * to all the positions on the screen
	 */
	public void clearScreen() {

		for (int x = 0; x < lenScreen; x++) {
//			screen[x].setCharAndAttr(' ', initAttr, false);
//			screen[x].setUseGUI(ScreenChar.NO_GUI);
			planes.setScreenCharAndAttr(x,' ', initAttr, false);
			planes.setUseGUI(x, ScreenChar.NO_GUI);
		}
		dirty.setBounds(tArea.getBounds());
		drawing = true;
      oia.clearScreen();

	}

	public void restoreScreen() {

		lastAttr = 32;
		dirty.setBounds(tArea.getBounds());
		updateImage(dirty);
	}

	/**
	 * Returns a pointer to the graphics area that we can draw on
	 *
	 * @return Graphics2D pointer of graphics buffer
	 */
	public Graphics2D getDrawingArea() {

		return bi.getDrawingArea();
	}

	protected synchronized void updateImage(int x, int y, int width, int height) {

		if (gg2d == null) {
			//System.out.println("was null");
			gg2d = (Graphics2D) gui.getGraphics();
		}

		// check for selected area and erase it before updating screen
		if (gui.rubberband != null && gui.rubberband.isAreaSelected()) {
			gui.rubberband.erase();
		}

		if (bi == null || gg2d == null) {
			if (bi == null)
				System.out.println("bi was null in update image");
			if (gg2d == null)
				System.out.println("gg2d was null in update image");
			return;
		}

		g2d.setClip(x, y, width, height);
		if (!cursorActive && x + width <= bi.getWidth(null)
				&& y + height <= (bi.getHeight(null) - fmWidth)) {
			paintComponent2(g2d);
		}

		//      if (tileimage != null) {
		//
		//         AlphaComposite ac =
		// AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
		//         g2d.setComposite(ac);
		//         g2d.drawImage(tileimage, null, null);
		//      }

		// LDC - WVL : 08/09/2003 : TR.000358
		// TN5250j overpaints superimposed components
		// as swing doesn't support overlay detection when painting a component
		// we have to adhere to swing's paint request and use dirty rectangle
		// marking
		// instead of off-thread painting
		// So we replaced the complete block underneath by 1 repaint request

		// fix for jdk1.4 - found this while testing under jdk1.4
		//   if the height and or the width are equal to zero we skip the
		//   the updating of the image.
		//      if (gui.isVisible() && height > 0 && width > 0) {
		//         bi.drawImageBuffer(gg2d,x,y,width,height);
		//      }
		//         if (gui.isVisible()) {
		//            if (height > 0 && width > 0) {

		// We now redraw the selected area rectangle.
		if (gui.rubberband != null && gui.rubberband.isAreaSelected()) {
			gui.rubberband.draw();
		}

		//         if (!fullRepaint) {
		//            bi.drawImageBuffer(gg2d,x,y,width,height);
		//         }
		//         else
		//            gui.repaint();

		//            System.out.println(" something went right finally " + gui.isVisible()
		// +
		//                           " height " + height + " width " + width);
		//         }
		//            else {
		//            bi.drawImageBuffer(gg2d);
		//            System.out.println(" something is wrong here " + gui.isVisible() +
		//                           " height " + height + " width " + width);

		//            }
		//      }
		if (x == 0)
			width += bi.offLeft;
		else
			x += bi.offLeft;
		if (y == 0)
			height += bi.offTop;
		else
			y += bi.offTop;

		gui.repaint(x, y, width, height);

	}

	protected void updateImage(Rectangle r) {
		updateImage(r.x, r.y, r.width, r.height);
	}

	protected void paintComponent3(Graphics g) {
		//      System.out.println("paint from screen");
		Graphics2D g2 = (Graphics2D) g;

		//      Rectangle r = g.getClipBounds();

		g2.setColor(colorBg);
		g2.fillRect(0, 0, gui.getWidth(), gui.getHeight());

		bi.drawImageBuffer(g2);
	}

   /**
    * Notify all registered listeners of the onScreenChanged event.
    *
    */
   private void fireScreenChanged(int which, int x, int y, int width, int height) {

      if (listeners != null) {
         int size = listeners.size();
         for (int i = 0; i < size; i++) {
            ScreenListener target =
                    (ScreenListener)listeners.elementAt(i);
            target.onScreenChanged(1,x,y,width,height);
         }
      }
   }

	protected void paintComponent2(Graphics2D g2) {

		if (bi == null) {
			paintComponent3(g2);
		}

		Rectangle r = g2.getClipBounds();

		if (r == null) {
			paintComponent3((Graphics) g2);
			return;
		}

		g2.setColor(colorBg);
		//      System.out.println("PaintComponent " + r);

		g2.fillRect(r.x, r.y, r.width, r.height);

		int sPos = getRowColFromPoint(r.x, r.y);

		int ePos = getRowColFromPoint(r.width, r.height) - numCols;
		// fix me here
		int er = (numRows - ((((fmHeight * (numRows + 1)) - ((r.y + r.height) + fmHeight)) / fmHeight)));
		int ec = (numCols - ((((fmWidth * (numCols + 1)) - ((r.x + r.width) + fmWidth)) / fmWidth)));

		//      int er1 = getRow(ePos);
		//      int ec2 = getCol(ePos);
		int sr = getRow(sPos);
		int c = getCol(sPos);
		er--;
		ec--;

      fireScreenChanged(1,sr,c,er,ec);

//         //      System.out.println(sr + "," + c + "," + er + "," + ec);
      workR.setBounds(sr, c, ec, er);

//         int rows = er - sr;
//         int cols = 0;
//         int lr = workR.x;
//         int lc = 0;
//
//         lr = sPos;
//
//         while (rows-- >= 0) {
//            cols = ec - c;
//            lc = lr;
//            while (cols-- >= 0) {
//               if (lc >= 0 && lc < lenScreen) {
//   //					screen[lc++].drawChar(g2);
//                  bi.drawChar(g2,screen[lc],getRow(lc),getCol(lc));
//                  lc++;
//               }
//            }
//            lr += numCols;
//         }

	}

	/**
	 *
	 * This routine will make sure we have something to draw on
	 *
	 */
	private void checkOffScreenImage() {

		// do we have something already?
		if (bi == null) {

			bi = new GuiGraphicBuffer(this);

			if (antialiased) {
				bi.setUseAntialias(true);
			}

			// allocate a buffer Image with appropriate size
			bi.getImageBuffer(fmWidth * numCols, fmHeight * (numRows + 2));

			// fill in the areas
			tArea = new Rectangle2D.Float(0, 0, 0, 0);
			cArea = new Rectangle2D.Float(0, 0, 0, 0);
			aArea = new Rectangle2D.Float(0, 0, 0, 0);
			sArea = new Rectangle2D.Float(0, 0, 0, 0);

			// Draw Operator Information Area
			drawOIA();
		}

	}

	/**
	 * Convinience method to resize the screen area such as when the parent
	 * frame is resized.
	 *
	 * @param width
	 * @param height
	 */
	private final void resizeScreenArea(int width, int height) {

		Font k = null;
		LineMetrics l;
		FontRenderContext f = null;
		k = GUIGraphicsUtils.getDerivedFont(font, width, height, numRows,
				numCols, sfh, sfw, ps132);
		f = new FontRenderContext(k.getTransform(), true, true);

		l = k.getLineMetrics("Wy", f);

		if (font.getSize() != k.getSize() || updateFont
				|| (bi.offLeft != (width - bi.getWidth()) / 2)
				|| (bi.offTop != (height - bi.getHeight()) / 2)) {

			// set up all the variables that are used in calculating the new
			// size
			font = k;
			FontRenderContext frc = new FontRenderContext(font.getTransform(),
					true, true);
			lm = font.getLineMetrics("Wy", frc);
			fmWidth = (int) font.getStringBounds("W", frc).getWidth() + 2;
			fmHeight = (int) (font.getStringBounds("g", frc).getHeight()
					+ lm.getDescent() + lm.getLeading());

			bi.resize(fmWidth * numCols, fmHeight * (numRows + 2));

			// set the offsets for the screen centering.
			bi.offLeft = (width - bi.getWidth()) / 2;
			bi.offTop = (height - bi.getHeight()) / 2;
			if (bi.offLeft < 0)
				bi.offLeft = 0;
			if (bi.offTop < 0)
				bi.offTop = 0;

			drawOIA();

			// and loop through the screen buffer to draw the new image with
			// the correct attributes
//			for (int m = 0; m < lenScreen; m++) {
//				screen[m].setRowCol(getRow(m), getCol(m));
//
//			}
			updateFont = false;
		}

	}

	/**
	 * Draw the Operator Information Area for feedback
	 */
	private void drawOIA() {

		// get ourselves a global pointer to the graphics
		g2d = bi.drawOIA();

		tArea.setRect(bi.getTextArea());
		cArea.setRect(bi.getCommandLineArea());
		aArea.setRect(bi.getScreenArea());
		sArea.setRect(bi.getStatusArea());

	}

	/**
	 *
	 * This routine will calculate the new image size that will be displayed
	 * when the frame that holds it is resized.
	 *
	 * The font characteristics are changed to fit the new size as will as the
	 * screen buffer row column offsets so that the characters that make of the
	 * screen are displayed correctly
	 *
	 * Changes made by Luc - LDC This routine was split into two separate
	 * functions. setBounds and repaintScreen. This allowed a public function
	 * called updateScreen to be introduced.
	 *
	 * @param width
	 * @param height
	 *
	 *
	 */

	public final void setBounds(int width, int height) {

		setCursorActive(false);
		resizeScreenArea(width, height);
		repaintScreen();
		setCursorActive(true);
	}

	/**
	 * This method does a complete refresh of the screen.
	 */
	public final void updateScreen() {
		repaintScreen();
		setCursorActive(false);
		setCursorActive(true);
	}

   /**
    * Add a ScreenListener to the listener list.
    *
    * @param listener  The ScreenListener to be added
    */
   public void addScreenListener(ScreenListener listener) {

      if (listeners == null) {
          listeners = new java.util.Vector(3);
      }
      listeners.addElement(listener);

   }

   /**
    * Remove a ScreenListener from the listener list.
    *
    * @param listener  The ScreenListener to be removed
    */
   public void removeScreenListener(ScreenListener listener) {

      if (listeners == null) {
          return;
      }
      listeners.removeElement(listener);
   }

	/**
	 * Utility method to share the repaint behaviour between setBounds() and
	 * updateScreen.
	 */
	private void repaintScreen() {
		drawing = true;
		dirty.setBounds(tArea.getBounds());
		if (gui.getGraphics() != null) {
			// do not forget to null out gg2d before update or else there will
			//    be a very hard to trace screen resize problem
			gg2d = null;
			updateDirty();
		}

		// restore statuses that were on the screen before resize
		if (oia.getLevel() == ScreenOIA.OIA_LEVEL_INPUT_ERROR) {
		   oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
		         			ScreenOIA.OIA_LEVEL_INPUT_ERROR);
		}

		if (oia.getLevel() == ScreenOIA.OIA_LEVEL_INPUT_INHIBITED) {
		   oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
		         			ScreenOIA.OIA_LEVEL_INPUT_INHIBITED);
		}

		if (oia.isMessageWait())
			oia.setMessageLightOn();
	}

	/**
	 *
	 * This routine will calculate the new image size that will be displayed
	 * when the frame that holds it is resized.
	 *
	 * The font characteristics are changed to fit the new size as will as the
	 * screen buffer row column offsets so that the characters that make of the
	 * screen are displayed correctly
	 *
	 * @param r
	 */
	public final void setBounds(Rectangle r) {

		setBounds(r.width, r.height);
	}

	/**
	 *
	 * This routine returns the preferred size of the component that wants to be
	 * displayed
	 *
	 * @return the value of the preferredSize property
	 *
	 */
	public final Dimension getPreferredSize() {

		return new Dimension(fmWidth * numCols, fmHeight * (numRows + 2));

	}

	/**
	 *
	 * This routine is responsible for setting up a PrinterJob on this component
	 * and initiating the print session.
	 *
	 */
	public final void printMe() {

		Thread printerThread = new PrinterThread(planes.screen, font, numCols,
				numRows, colorBg, defaultPrinter, (Session) gui);

		printerThread.start();

	}

//	// ADDED BY BARRY
//	public ScreenChar[] getCharacters() {
//		return this.screen;
//	}
	// ADDED BY BARRY - changed by Kenneth to use the character plane
   //  This should be replaced with the getPlane methods when they are implemented
	public char[] getCharacters() {
		return planes.screen;
	}

	public Gui5250 getGui() {
		return this.gui;
	}
}