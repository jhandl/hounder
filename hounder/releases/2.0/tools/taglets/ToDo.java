import java.util.Map;

public class ToDo extends ListTag
{
	public static void register(Map tagletMap) {
		ListTag.register(tagletMap, new ToDo());
	}

	/**
	 * Create a new ListTag, with tag name 'todo'.  Default
	 * the tag header to 'To Do:' and default to an 
	 * unordered list.
	 *
	 * @todo a single todo entry
	 */
	public ToDo() {
		super("todo", "todo", "To Do:", ListTag.UNORDERED_LIST);
	}

	protected String getFgColor(final String propName) {
		return "#FF0000";
	}

	protected String getBgColor(final String propName) {
		if (propName.equals("text")) {
			return "#FFF000";
		} else {
			return super.getBgColor(propName);
		}
	}

}
