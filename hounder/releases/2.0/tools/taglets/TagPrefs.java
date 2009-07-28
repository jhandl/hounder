import java.util.prefs.Preferences;

class TagPrefs {
	private static final Preferences prefs = Preferences.userNodeForPackage(TagPrefs.class);

	static final String PREF_NOVALUE = "-";

	private final String prefix;

	TagPrefs(String prefix) {
		if (prefix == null)
			throw new NullPointerException("TagPrefs requires a non-null prefs prefix");
		this.prefix = prefix;
	}

	String prefix() {
		return this.prefix;
	}

	static void flush() throws java.util.prefs.BackingStoreException {
		TagPrefs.prefs.flush();
	}

	/**
	 * Force a pref to exist in the external prefs DB.  Give it
	 * the default value, if it doesn't already have value.
	 */
	void forcePref(String prefName, String defaultValue) {
		String realPrefName = this.prefix + "." + prefName;

		String val = prefs.get(realPrefName, defaultValue);
		prefs.put(realPrefName, val);
	}

	/**
	 * Return the value associated with the given pref.  Returns
	 * PREF_NOVALUE as the default.
	 */
	String getPref(String prefName) {
		String realPrefName = this.prefix + "." + prefName;
		String val = prefs.get(realPrefName, PREF_NOVALUE);
		return val;
	}
}
