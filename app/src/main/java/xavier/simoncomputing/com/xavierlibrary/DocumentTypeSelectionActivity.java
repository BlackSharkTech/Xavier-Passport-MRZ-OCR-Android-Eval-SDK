package xavier.simoncomputing.com.xavierlibrary;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.HashMap;


/**
 *  Displays Document Type Activity to allow the user to select
 *  the Document Type when we failed to OCR the Document Type.
 *
 */
public class DocumentTypeSelectionActivity extends Activity {
    public static final String LOG_TAG = "DocumentTypeSelectionActivity";

    private HashMap<String, String> mMrzKeyValueMap;
    private String mSelectedDocType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_type_selection);
        setTitle("Document Type Selection");
        Intent intent = getIntent();
        mMrzKeyValueMap = (HashMap<String, String>) intent.getSerializableExtra(MainActivity.MRZ_KEY_VALUE_MAP);

    }

    /**
     *
     */
    @Override
    protected void onResume() {
        super.onResume();

        initDocumentTypeSpinner();

    }


    /**
     *
     */
    private void initDocumentTypeSpinner() {

        Spinner spinner = (Spinner) findViewById(R.id.documentTypeSpinner);
        final MrzDocumentType items[] = new MrzDocumentType[2];

        // For now we handle only "P" and "ID"
        items[0] = new MrzDocumentType("P - Passport", "P");
        items[1] = new MrzDocumentType("ID", "ID");

        ArrayAdapter<MrzDocumentType> adapter =
                new ArrayAdapter<MrzDocumentType>(
                        this,
                        R.layout.spinner_item,
                        items);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        MrzDocumentType docType = items[position];
                        mSelectedDocType = docType.getValue();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

    }


    /**
     *
     * @param view
     */
    public void submit(View view) {

        Intent returnIntent = new Intent(DocumentTypeSelectionActivity.this, MainActivity.class);

        if(mMrzKeyValueMap.containsKey("documentType")) {
            mMrzKeyValueMap.put("documentType", mSelectedDocType);
            returnIntent.putExtra(MainActivity.MRZ_KEY_VALUE_MAP, mMrzKeyValueMap);
            setResult(RESULT_OK,returnIntent);
        }
        else {
            setResult(RESULT_CANCELED, returnIntent);
        }

        finish();
    }


    /**
     *
     * @param view
     */
    public void cancel(View view) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        cleanup();
        finish();
    }


    /**
     *
     */
    private void cleanup() {
        if(mMrzKeyValueMap != null) {
            mMrzKeyValueMap.clear();
            mMrzKeyValueMap = null;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    /**
     *
     */
    private class MrzDocumentType {
        private String name;
        private String value;

        public MrzDocumentType( String name, String value ) {
            this.name = name;
            this.value = value;
        }

        public String getSpinnerName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public String toString() {
            return name;
        }

    }

}
