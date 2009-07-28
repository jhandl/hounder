import com.sun.tools.doclets.Taglet;    
import com.sun.javadoc.*;
import java.util.Map;
import java.util.HashMap;

public abstract class ListTag implements Taglet {
	public static final ListType ORDERED_LIST = new ListType("ordered", "<ol>", "</ol>", "<li>", "</li>");
	public static final ListType UNORDERED_LIST = new ListType("unordered", "<ul>", "</ul>", "<li>", "</li>");
	public static final ListType TABLE_LIST = new ListType("table", "<table cellpadding=\"2\">", "</table>", "<tr>", "</tr>");
	public static final ListType VISIBLETABLE_LIST = new ListType("table", "<table BORDER=\"1\" CELLPADDING=\"2\">", "</table>", "<tr>", "</tr>");

	protected static final String LISTTYPE = "listtype";
	protected static final String HEADER_TEXT = "header.text";
	protected static final String TEXT_FGCOLOR = "text.color.fg";
	protected static final String TEXT_BGCOLOR = "text.color.bg";
	protected static final String TEXT_RELSIZE = "text.relsize";

	private final String tagName;

	protected final TagPrefs tagPrefs;

	private static class ListType {
		private final String type;
		private final String s;
		private final String e;
		private final String entryS;
		private final String entryE;

		private static final HashMap nameToType = new HashMap(5);

		ListType(String type, String s, String e, String entryS, String entryE) {
			this.type = type;
			this.s = s;
			this.e = e;
			this.entryS = entryS;
			this.entryE = entryE;
			
			nameToType.put(this.mapKey(), this);
		}

		public String getStartHtml() {
			return this.s;
		}
		
		public String getEndHtml() {
			return this.e;
		}

		public String getEntryStartHtml() {
			return this.entryS;
		}

		public String getEntryEndHtml() {
			return this.entryE;
		}
		
		public String mapKey() {
			return this.type;
		}

		public static ListType lookup(String key) {
			ListType lt;
			lt = (ListType)(nameToType.get(key));
			return lt;
		}
	}


	/**
	 * Create a new list-behaviour tag.  The <code>tagHeader</code> and
	 * <code>listType</code> parameters can be overridden in properties.
	 */
	public ListTag(String tagName, String tagHeader, ListType listType) {
		this(tagName, tagName, tagHeader, listType);
	}
	
	public ListTag(String tagName, String prefsName, String tagHeader, ListType listType) {
		this.tagName = tagName;
		this.tagPrefs = new TagPrefs(prefsName);

		forceDefaultPrefs(tagHeader, listType);
	}
	
	protected void forceCustomDefaultPrefs(TagPrefs tagPrefs) throws Exception {
	}

	protected void forceColorPrefs(TagPrefs tagPrefs, String tagPrefix) {
		tagPrefs.forcePref(tagPrefix + ".color.fg", TagPrefs.PREF_NOVALUE);
		tagPrefs.forcePref(tagPrefix + ".color.bg", TagPrefs.PREF_NOVALUE);
		tagPrefs.forcePref(tagPrefix + ".relsize", TagPrefs.PREF_NOVALUE);
	}

	private void forceDefaultPrefs(String tagHeader, ListType listType) {
		try {
			tagPrefs.forcePref(LISTTYPE, listType.mapKey());
			tagPrefs.forcePref(HEADER_TEXT, tagHeader);
			forceColorPrefs(tagPrefs, "header");
			forceColorPrefs(tagPrefs, "text");
			forceCustomDefaultPrefs(tagPrefs);
			tagPrefs.flush();
		} catch (Exception e) {
			System.err.println("(ignored) prefs exception: " +e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Register the given taglet in the given map.  Uses
	 * the ListTag.name() to get the name of the tag.
	 */
	public static void register(Map tagletMap, ListTag lt) {
		Taglet oldt = (Taglet) tagletMap.get(lt.tagName);
		if (oldt != null) {
			System.err.println("Warning(ListTag): replacing taglet " +oldt+ " with " +lt+ ".");
			tagletMap.remove(lt.tagName);
		}
		tagletMap.put(lt.tagName, lt);
	}
	
	public String getName() {
		return this.tagName;
	}

	public boolean isInlineTag() {
		return false;
	}
	
	public boolean inField() {
		return true;
	}
	
	public boolean inConstructor() {
		return true;
	}
	
	public boolean inMethod() {
		return true;
	}
	
	public boolean inType() {
		return true;
	}
	
	public boolean inPackage() {
		return true;
	}
	
	public boolean inOverview() {
		return true;
	}
	
	protected String getFgColor(final String propName) {
		return tagPrefs.getPref(propName + ".color.fg");
	}
	protected String getBgColor(final String propName) {
		return tagPrefs.getPref(propName + ".color.bg");
	}
	protected String getRelSize(final String propName) {
		return tagPrefs.getPref(propName + ".relsize");
	}



	/**
	 * Format the given text using the properties under the givne propName into
	 * the given StringBuffer.
	 */
	protected void formatText(StringBuffer sbuf, String text, String propName) {
		String bgcolor = getBgColor(propName);
		String fgcolor = getFgColor(propName);
		String relsize = getRelSize(propName);

		boolean hasBgColor = false;
		if (! bgcolor.equals(TagPrefs.PREF_NOVALUE)) {
			sbuf.append("<table><tr><td bgcolor=\"")
				.append(bgcolor)
				.append("\">");
			hasBgColor = true;
		}

		if (fgcolor.equals(TagPrefs.PREF_NOVALUE) && relsize.equals(TagPrefs.PREF_NOVALUE)) {
			sbuf.append(text);
		} else {
			sbuf.append("<font ");
			if (! fgcolor.equals(TagPrefs.PREF_NOVALUE))
				sbuf.append("color=\"").append(fgcolor).append("\" ");
			if (! relsize.equals(TagPrefs.PREF_NOVALUE))
				sbuf.append("size=\"").append(relsize).append("\" ");
			sbuf.append(">")
				.append(text)
				.append("</font>");
		}
		
		if (hasBgColor)
			sbuf.append("</td></tr></table>");
	}

	/**
	 * Generate formatted HTML for the given tag text.  Put the
	 * HTML in the given StringBuffer.
	 *
	 * @return the given StringBuffer.
	 */
	protected void parseTagText(StringBuffer sbuf, String text, boolean multi) {
		String listTypeKey = tagPrefs.getPref(LISTTYPE);
		ListType listType = ListType.lookup(listTypeKey);
		boolean doTable = false;
		
		if (multi 
		    && (listType == ListTag.TABLE_LIST))
			doTable = true;
		
		if (doTable)
			sbuf.append("<td>");
		formatText(sbuf, text, "text");
		if (doTable)
			sbuf.append("</td>\n");
	}
	
	/**
	 * Override to insert custom text after the list start,
	 * but before the first bit of tag text
	 */
	protected void emitCustomHeader(StringBuffer sbuf, boolean multi) {
	}

	/**
	 * Override to insert custom text after the list is complete,
	 * but before the list closing tags
	 */
	protected void emitCustomFooter(StringBuffer sbuf, boolean multi) {
	}

	/**
	 * Emit header for HTML version of tag.  All generated text is
	 * put in the given sbuf.  The multi parameter indicates if
	 * this header is for more than one element.
	 */
	protected void emitHeader(StringBuffer sbuf, boolean multi) {
		String tagHeader = tagPrefs.getPref(HEADER_TEXT);
		String listTypeKey = tagPrefs.getPref(LISTTYPE);
		ListType listType = ListType.lookup(listTypeKey);

		sbuf.append("<dt><b>");
		formatText(sbuf, tagHeader, "header");
		sbuf.append("</b></dt>")
			.append("<dd>\n");
		if (multi)
			sbuf.append(listType.getStartHtml());
		sbuf.append("\n");

		emitCustomHeader(sbuf, multi);
	}

	protected void emitTag(Tag tag, StringBuffer sbuf, boolean multi) {
		String listTypeKey = tagPrefs.getPref(LISTTYPE);
		ListType listType = ListType.lookup(listTypeKey);

		if (multi)
			sbuf.append(listType.getEntryStartHtml());
		parseTagText(sbuf, tag.text(), multi);
		if (multi)
			sbuf.append(listType.getEntryEndHtml());
	}
	
	/**
	 * Emit footer for HTML version of tag.  All generated text is
	 * put in the given sbuf.  The multi parameter indicates if
	 * this header is for more than one element.
	 */
	protected void emitFooter(StringBuffer sbuf, boolean multi) {
		String listTypeKey = tagPrefs.getPref(LISTTYPE);
		ListType listType = ListType.lookup(listTypeKey);

		emitCustomFooter(sbuf, multi);
		
		if (multi)
			sbuf.append(listType.getEndHtml());
		sbuf.append("</dd>\n");
	}

	protected void startingTags() {
	}
	
	protected void endingTags(StringBuffer sbuf) {
	}

	public String toString(Tag tag)
	{
		StringBuffer sbuf = new StringBuffer(1000);

		// XXX make it an option to emit single entries with the list header/etc.

		startingTags();

		emitHeader(sbuf, false);
		emitTag(tag, sbuf, false);
		emitFooter(sbuf, false);

		endingTags(sbuf);

		return sbuf.toString();
	}

	public String toString(Tag[] tags) {
		if (tags.length == 0)
			return "";

		StringBuffer sbuf = new StringBuffer(200 + (800*tags.length));

		startingTags();
		
		emitHeader(sbuf, true);
		for (int i = 0; i < tags.length; i++)
			emitTag(tags[i], sbuf, true);
		emitFooter(sbuf, true);

		endingTags(sbuf);
		
		return sbuf.toString();
	}
}
