import com.sun.tools.doclets.Taglet;
import com.sun.javadoc.*;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.HashSet;
import java.util.Date;
import java.util.Iterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * <h2>Usage</h2>
 * <pre>
 * 	&#064;reviewedby &lt;name&gt; &lt;date&gt; [notes ...]
 * </pre>
 *
 * Indicate a code review for the associated class/interface was
 * completed by a reviewer.  <code>name</code> is a unique name for a
 * reviewer (for example, an appropriate login name).  The
 * <code>date</code> is a <code>YYYY/MM/DD</code> style date string.
 * Each must be a single token.  Any <code>[notes ...]</code> are
 * optional.
 * 
 * <p><code>&#064;reviewedby</code> is only recognized on class/interface
 * level comments.
 * 
 * <p>HTML in the generated javadoc can include provided reviews,
 * missing reviews, or both.  For provided reviews, a full or compact
 * format is supported.
 * 
 * <p>The set of expected IDs and due dates are set via JDK1.4
 * <code>java.util.prefs</code> preferences.  See below for a
 * description of the preferences.
 *
 * <p> See the generated javadoc below for an example of what this
 * might look like.
 */
public class ReviewedBy extends ListTag {
	/**
	 * Full set of IDs.
	 */
	private final HashSet allUIDs = new HashSet();

	/**
	 * Set of IDs missing in this pass.
	 */
	private HashSet missingIDs;
	

	private final Date completedSince;
	private final boolean showCompleted;
	private final boolean showMissing;
	
	private static final String UIDS = "review.uids";
	private static final String IN_DATEFORMAT = "review.date.format";
	private static final String SHOW_COMPLETED = "show.completed";
	private static final String SHOW_COMPLETED_SINCE = "show.completed.since";
	private static final String SHOW_MISSING = "show.missing";
	private static final String OUT_DATEFORMAT = "html.date.format";


	public static void register(Map tagletMap) {
		ListTag.register(tagletMap, new ReviewedBy());
	}

	public ReviewedBy() {
		this("default");
	}


	/**
	 * Create a new ListTag, with tag name 'reviewedby'.  Default
	 * the tag header to 'Reviewed By:' and default to an
	 * unordered list.
	 */
	public ReviewedBy(String projectName) {
		super("reviewedby", "reviewedby." + projectName, 
		      "Reviewed By:", ListTag.TABLE_LIST);

		String uidList = super.tagPrefs.getPref(UIDS);
		if (! uidList.equals(TagPrefs.PREF_NOVALUE)) {
			String[] uids = uidList.split("\\s*,\\s*");
			for (int i = 0; i < uids.length; i++)
				this.allUIDs.add(uids[i]);
		}

		// Extract the flags for showing missing/completed reviews
		
		this.showCompleted = Boolean.valueOf(super.tagPrefs.getPref(SHOW_COMPLETED)).booleanValue();
		this.showMissing = Boolean.valueOf(super.tagPrefs.getPref(SHOW_MISSING)).booleanValue();

		// Extract the completed-by-date, if one

		String completedSinceStr = super.tagPrefs.getPref(SHOW_COMPLETED_SINCE);

		if (completedSinceStr.equals(tagPrefs.PREF_NOVALUE))
			this.completedSince = null;
		else {
			Date d = null;

			try
			{
				d = DateFormat.getDateInstance().parse(completedSinceStr);
			}
			catch (java.text.ParseException pe)
			{
				System.err.println("WARNING(" +super.tagPrefs.prefix()+ ") "
						   + "Cannot parse date string" + completedSinceStr);
			}
			this.completedSince = d;
		}
	}

	protected void startingTags() {
		this.missingIDs = (HashSet)(allUIDs.clone());
	}
	
	protected void endingTags(StringBuffer sbuf) {
		if (this.showMissing) {
			Iterator i = this.missingIDs.iterator();
			boolean doClose = false;

			if (i.hasNext()) {
				//sbuf.append("<tr><th align=\"left\" colspan=\"3\">Missing Reviews:</th></tr>\n")
				// .append("<tr><td colspan=\"3\">\n");
				
				sbuf.append("<dt><b>");
				super.formatText(sbuf, "Missing Reviews:", "html.missing.header");
				sbuf.append("</b></dt>\n")
					.append("<dd>\n");
				
				doClose = true;
			}
			
			while (i.hasNext()) {
				String missingID = (String)(i.next());
				sbuf.append(missingID);
				if (i.hasNext())
					sbuf.append(", ");
			}
			
			if (doClose) {
				// sbuf.append("</td></tr>\n");
				sbuf.append("</dd>\n");
			}
		}
		this.missingIDs = null;
	}

	protected void forceCustomDefaultPrefs(TagPrefs tagPrefs) {
		super.forceColorPrefs(tagPrefs, "html.missing.header");
		tagPrefs.forcePref(UIDS, TagPrefs.PREF_NOVALUE);
		tagPrefs.forcePref(IN_DATEFORMAT, "yy/MM/dd");
		tagPrefs.forcePref(OUT_DATEFORMAT, "MMMM d, yyyy");
		tagPrefs.forcePref(SHOW_COMPLETED_SINCE, TagPrefs.PREF_NOVALUE);
		tagPrefs.forcePref(SHOW_COMPLETED, "true");
		tagPrefs.forcePref(SHOW_MISSING, "true");
	}

	/**
	 * Override
	 */
	protected void emitCustomHeader(StringBuffer sbuf, boolean multi) {
		if (!multi)
			sbuf.append("<table>\n");

		if (this.showCompleted) {
			sbuf.append("<tr>")
				.append("<th>User</th>")
				.append("<th>Date</th>")
				.append("<th>Comment</th>")
				.append("</tr>")
				.append("\n");
		}
	}

	private void emitReviewEntry(StringBuffer sbuf, String uid, String date, String notes) {
		sbuf.append("  <td><code>");
		super.formatText(sbuf, uid, "text");
		sbuf.append("</code></td>\n")
			.append("  <td><font size=\"-1\">");
		super.formatText(sbuf, date, "text");
		sbuf.append("</font></td>\n")
			.append("  <td>");
		super.formatText(sbuf, notes, "text");
		sbuf.append("</td>\n");
	}

	/**
	 * Override
	 */
	protected void emitCustomFooter(StringBuffer sbuf, boolean multi) {
		if (!multi)
			sbuf.append("</table>\n");
	}

	String formatDate(String inputDate) {
		try {
			// DateFormat idf = DateFormat.getDateInstance(DateFormat.SHORT);
			SimpleDateFormat idf = new SimpleDateFormat(super.tagPrefs.getPref(IN_DATEFORMAT));
			
			Date date = idf.parse(inputDate);
		
			SimpleDateFormat odf = new SimpleDateFormat(super.tagPrefs.getPref(OUT_DATEFORMAT));

			return odf.format(date);
		} catch (java.text.ParseException pe) {
			return inputDate;
		}
	}

	/**
	 * Override parsing of tag text to pull out table stuff.
	 */
	protected void parseTagText(StringBuffer sbuf, String text, boolean multi) {
		//System.err.println("@reviewedby parse: " +text);
		
		String[] splitted = text.split("\\s+", 3);

		String[] parsed = new String[3];
		
		if (splitted.length > 0)
			parsed[0] = splitted[0];
		else
			parsed[0] = "UNKNOWN";
		
		if (splitted.length > 1)
			parsed[1] = formatDate(splitted[1]);
		else
			parsed[1] = "N/A";

		if (splitted.length > 2)
			parsed[2] = splitted[2];
		else
			parsed[2] = "";
		
		if (this.missingIDs.contains(parsed[0]))
			this.missingIDs.remove(parsed[0]);
		else
			System.err.println("WARNING(" +super.tagPrefs.prefix() + ") unexpected id: " +parsed[0]);

		if (this.showCompleted)
			emitReviewEntry(sbuf, parsed[0], parsed[1], parsed[2]);
	}

	// Override definitions of when this tag is useful.
	
	public boolean isInlineTag() {
		return false;
	}
	
	public boolean inField() {
		return false;
	}
	
	public boolean inConstructor() {
		return false;
	}
	
	public boolean inMethod() {
		return false;
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
}
