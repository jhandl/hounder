import java.util.Map;

public class FixMe extends ListTag
{
	public static void register(Map tagletMap) {
		ListTag.register(tagletMap, new FixMe());
	}

	/**
	 * Create a new ListTag, with tag name 'todo'.  Default
	 * the tag header to 'To Do:' and default to an 
	 * unordered list.
	 *
	 * @todo a single todo entry
	 */
	public FixMe() {
		super("fixme", "fixme", "To Fix:", ListTag.UNORDERED_LIST);
	}

	protected String getFgColor(final String propName) {
		if (propName.equals("text"))
			return "#000000";
		else
			return "#FF0000";
	}

	protected String getBgColor(final String propName) {
		if (propName.equals("text")) {
			return "#FF7777";
		} else {
			return super.getBgColor(propName);
		}
	}

}
