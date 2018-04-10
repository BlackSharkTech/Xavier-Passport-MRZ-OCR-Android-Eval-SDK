package xavier.blacksharktech.com.xavierlibrary;

public enum ValidDocumentTypes {PASSPORT_DOC_TYPE("P"), ID_DOC_TYPE("ID"), UNKNOWN("UNKNOWN"), BARCODE("B");

    private String mValue;

    /**
     *
     * @param value
     */
    ValidDocumentTypes(String value) {
        this.mValue = value;
    }


    /**
     * @return
     */
    public String getValue() {
        return this.mValue;
    }


    /**
     * @param documentType
     * @return
     */
    public static ValidDocumentTypes fromString(String documentType) {
        ValidDocumentTypes foundDocumentType = UNKNOWN;
        if (documentType != null) {
            for (ValidDocumentTypes validDocumentType : ValidDocumentTypes.values()) {
                if (documentType.equalsIgnoreCase(validDocumentType.mValue)) {
                    foundDocumentType = validDocumentType;
                    break;
                }
            }
        }
        return foundDocumentType;
    }
}
