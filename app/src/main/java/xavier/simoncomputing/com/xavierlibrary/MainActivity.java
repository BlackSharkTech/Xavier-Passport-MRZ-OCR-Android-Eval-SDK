package xavier.simoncomputing.com.xavierlibrary;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;

import xavier.simoncomputing.com.xavierlib.XavierActivity;
import xmlwise.Plist;


public class MainActivity extends FragmentActivity {

    private static final String LOG_TAG = "MainActivity";

    public static final String MRZ_KEY_VALUE_MAP = "MrzKeyValueMap";

    public static final String PASSPORT_DOC_TYPE = "P";
    public static final String ID_DOC_TYPE = "ID";

    public static final int MRZ_REQUEST = 1;
    public static final int USER_DOCTYPE_SELECTION_REQUEST = 2;

    private ImageButton mStartXavierBtn;
    private ImageButton mStopBtn;
    private  LinearLayout mDocumentTypeInflaterLayout;



    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(LOG_TAG, "+++ onCreate() +++");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mDocumentTypeInflaterLayout = (LinearLayout) findViewById(R.id.documentTypeInflaterLayout);

        // Start button
        // ------------
        mStartXavierBtn = (ImageButton) findViewById(R.id.startXavier);
        mStartXavierBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                startXavierActivity();

            }
        });



        // Stop button
        // -----------
        mStopBtn = (ImageButton) findViewById(R.id.stop);
        mStopBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


    /**
     *
     */
    private void startXavierActivity() {
        Intent intent = new Intent(MainActivity.this, XavierActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        HashMap<String, Object> properties = null;
        try {
            InputStream inputStream =getResources().openRawResource(R.raw.xavier);
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                properties = (HashMap<String, Object>)Plist.fromXml(sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                br.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // WARNING: This is not a valid key !
        // Please go to http://www.simoncomputing.com/main/xavier/ to request for
        // an evaluation or production license key
        // ----------------------------------------------------------------------
//        intent.putExtra(XavierActivity.LICENSE_KEY, "E12345678");
//        intent.putExtra(XavierActivity.EMAIL_ADDRESS, "test@hotmail.com");
        intent.putExtra(XavierActivity.SETTINGS, properties);

        startActivityForResult(intent, MRZ_REQUEST);

    }



    /**
     *
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(LOG_TAG, "+++ onActivityResult() +++");

        if (requestCode == MRZ_REQUEST) {

            if(resultCode == RESULT_OK) {

                String mrzElements = (String) data.getSerializableExtra(XavierActivity.MRZ_LINES);

                if(mrzElements == null || mrzElements.length() == 0) {
                    Log.e(LOG_TAG, "No MRZ lines");
                    mDocumentTypeInflaterLayout.setVisibility(View.GONE);

                }
                else {
                    // We have MRZ lines to display
                    mDocumentTypeInflaterLayout.setVisibility(View.VISIBLE);
                    processMrzResult(mrzElements);

                }

            }
            else if(resultCode == RESULT_CANCELED) {

                mDocumentTypeInflaterLayout.setVisibility(View.GONE);

                // Encounters Xavier library error: log, display, or send email error message to help desk
                String errorMessage = (String) data.getSerializableExtra(XavierActivity.ERROR_MESSAGE);
                if(errorMessage != null || !errorMessage.isEmpty()) {
                    displayError(this, errorMessage);
                }
            }
            
        }
        else if (requestCode == USER_DOCTYPE_SELECTION_REQUEST) {

            if(resultCode == RESULT_OK){
                // After the user select "P" or "ID" from the Valid Document Types drop down selecion
                HashMap<String, String> mrzKeyValueMap = (HashMap<String, String>) data.getSerializableExtra(MRZ_KEY_VALUE_MAP);

                // Inflate the view base on the user selection
                inflateView(mrzKeyValueMap);

            }
        }

    }


    /**
     *
     * @param context
     * @param errorMessage
     */
    public void displayError(Context context, final String errorMessage) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                builder.setTitle("Xavier Error");
                builder.create();
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                    }
                });
                builder.setMessage(errorMessage);
                builder.show();
                Looper.loop();
            }
        }.start();
    }


    /**
     *
     * @param mrzElements
     */
    private void processMrzResult(String mrzElements) {


        Log.d(LOG_TAG, "Received MRZ Result from Xavier Library: \n");
        Log.d(LOG_TAG, mrzElements);

        // Start parsing the MRZ string elements
        mrzElements = mrzElements.substring(1, mrzElements.length() - 1);
        HashMap<String, String> mrzKeyValueMap = parseMrzString(mrzElements);


        // Inflate the view accordingly, if for some reasons the parsed data has errors and
        // we could not determine if it is a "Passport" (two-line document) or "ID" (three-line document) type,
        // we display the Document Type Selection (DocumentTypeSelectionActivity) for the user to select
        // the type of the document.  You can simulate this scenario by tilting the Passport document at an angle
        // when scanning the MRZ document so that the "P" letter is missing from the MRZ scanning view.

        if(isValidDocumentType(mrzKeyValueMap)) {
            inflateView(mrzKeyValueMap);

        }
        else {

            // Unknown document type
            // ---------------------
            // Requests the user to select the Document Type.
            // The other option is to request the user to rescan the document
            // when it fails to determine the Document Type.
            displayDocumentTypeSelection(mrzKeyValueMap);
        }
    }


    /**
     *
     * @param mrzKeyValueMap
     * @return
     */
    private boolean isValidDocumentType(HashMap<String, String> mrzKeyValueMap) {

        String docType = mrzKeyValueMap.get("documentType");

        if(docType != null) {
            if (ValidDocumentTypes.fromString(docType) == ValidDocumentTypes.UNKNOWN) {
                return false;
            } else {
                return true;
            }
        }

        return false;
    }


    /**
     *  Determines the documentType and inflate the appropriate layout (id_layout_inflater or passport_layout_inflater
     *
     * @param mrzKeyValueMap
     */
    private void inflateView(HashMap<String, String> mrzKeyValueMap) {

        Log.d(LOG_TAG, "+++ inflateView() +++");

        removeAllInflatedViews();

        String docType = mrzKeyValueMap.get("documentType");

        if (docType != null && docType.compareTo(PASSPORT_DOC_TYPE) == 0) {

            inflatePassportLayout(mrzKeyValueMap);

        } else if (docType != null && docType.compareTo(ID_DOC_TYPE) == 0) {

            inflateIDLayout(mrzKeyValueMap);

        }

    }


    /**
     *  Launch the Document Type Selection Activity for the user to select the document type
     *  This is used when there is some MRZ scanning error and the document type is unknown.
     * @param mrzKeyValueMap
     */
    private void displayDocumentTypeSelection(final HashMap<String, String> mrzKeyValueMap) {

        Log.d(LOG_TAG, "+++ requestDocumentType() +++");

        Intent intent = new Intent(MainActivity.this, DocumentTypeSelectionActivity.class);
        intent.putExtra(MRZ_KEY_VALUE_MAP, mrzKeyValueMap);
        startActivityForResult(intent, USER_DOCTYPE_SELECTION_REQUEST);

    }


    /**
     * Parses MRZ elements
     *
     * @param mrz
     * @return
     */
    public static HashMap<String, String> parseMrzString(String mrz) {
        Log.d(LOG_TAG, "+++ parseMrzString() +++");

        HashMap<String, String> mrzKeyValueMap = new HashMap<String, String>();

        Log.d(LOG_TAG, "==========================\n");

        StringTokenizer st = new StringTokenizer(mrz, "=,");
        while (st.hasMoreTokens()) {

            String key = st.nextToken();

            key = key.trim();

            String val = st.nextToken();
            if (val == null) {
                val = "";
            } else {
                val = val.replace("<", " ");
                val = val.trim();
            }

            mrzKeyValueMap.put(key, val);
            Log.d(LOG_TAG, "[" + key + "] = " + val);
        }

        Log.d(LOG_TAG, "==========================\n");
        return mrzKeyValueMap;
    }


    /**
     * @param resource
     */
    private void inflateView(final int resource) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                View view = getLayoutInflater().inflate(resource, mDocumentTypeInflaterLayout, false);
                mDocumentTypeInflaterLayout.addView(view);
            }
        });
    }



    /**
     *
     */
    private void clearAllTextFields() {

        EditText documentType = (EditText) findViewById(R.id.documentType);
        if(documentType != null) {
            documentType.setText("");
        }

        EditText subType = (EditText) findViewById(R.id.subType);
        if(subType != null) {
            subType.setText("");
        }

        EditText org = (EditText) findViewById(R.id.org);
        if(org != null) {
            org.setText("");
        }

        EditText name = (EditText) findViewById(R.id.name);
        if(name != null) {
            name.setText("");
        }

        EditText number = (EditText) findViewById(R.id.number);
        if(number != null) {
            number.setText("");
        }

        EditText nationality = (EditText) findViewById(R.id.nationality);
        if(nationality != null) {
            nationality.setText("");
        }

        EditText birthDate = (EditText) findViewById(R.id.birthDate);
        if(birthDate != null) {
            birthDate.setText("");
        }

        EditText sex = (EditText) findViewById(R.id.sex);
        if(sex != null) {
            sex.setText("");
        }

        EditText expirationDate = (EditText) findViewById(R.id.expirationDate);
        if(expirationDate != null) {
            expirationDate.setText("");
        }

        EditText personal = (EditText) findViewById(R.id.personal);
        if(personal != null) {
            personal.setText("");
        }

        EditText issuingCountry = (EditText) findViewById(R.id.issuingCountry);
        if(issuingCountry != null) {
            issuingCountry.setText("");
        }

        EditText cardNumber = (EditText) findViewById(R.id.cardNumber);
        if(cardNumber != null) {
            cardNumber.setText("");
        }

        EditText transactionCode = (EditText) findViewById(R.id.transactionCode);
        if(transactionCode != null) {
            transactionCode.setText("");
        }

        EditText dateOfBirth = (EditText) findViewById(R.id.dateOfBirth);
        if(dateOfBirth != null) {
            dateOfBirth.setText("");
        }

        EditText gender = (EditText) findViewById(R.id.gender);
        if(gender != null) {
            gender.setText("");
        }

    }


    /**
     *  Parses and displays Passport MRZ elements
     */
    private void inflatePassportLayout(final HashMap<String, String> mrzKeyValueMap) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                inflateView(R.layout.passport_layout_inflater);

                clearAllTextFields();

                if (mrzKeyValueMap.get("documentType") != null) {
                    ((EditText) findViewById(R.id.documentType)).setText(mrzKeyValueMap.get("documentType"));
                }

                if (mrzKeyValueMap.get("subtype") != null) {
                    ((EditText) findViewById(R.id.subType)).setText(mrzKeyValueMap.get("subtype"));
                }

                if (mrzKeyValueMap.get("org") != null) {

                    ((EditText) findViewById(R.id.org)).setText(mrzKeyValueMap.get("org"));
                }

                if (mrzKeyValueMap.get("name") != null) {

                    ((EditText) findViewById(R.id.name)).setText(mrzKeyValueMap.get("name"));
                }

                if (mrzKeyValueMap.get("number") != null) {

                    ((EditText) findViewById(R.id.number)).setText(mrzKeyValueMap.get("number"));
                }

                if (mrzKeyValueMap.get("nationality") != null) {

                    ((EditText) findViewById(R.id.nationality)).setText(mrzKeyValueMap.get("nationality"));
                }

                if (mrzKeyValueMap.get("birthdate") != null) {

                    ((EditText) findViewById(R.id.birthDate)).setText(mrzKeyValueMap.get("birthdate"));
                }

                if (mrzKeyValueMap.get("sex") != null) {

                    ((EditText) findViewById(R.id.sex)).setText(mrzKeyValueMap.get("sex"));
                }

                if (mrzKeyValueMap.get("expiration_date") != null) {

                    ((EditText) findViewById(R.id.expirationDate)).setText(mrzKeyValueMap.get("expiration_date"));
                }
                if (mrzKeyValueMap.get("personal") != null) {

                    ((EditText) findViewById(R.id.personal)).setText(mrzKeyValueMap.get("personal"));
                }

            }
        });
    }


    /**
     * Parses and displays ID MRZ elements
     */
    private void inflateIDLayout(final HashMap<String, String> mrzKeyValueMap) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                inflateView(R.layout.id_layout_inflater);

                clearAllTextFields();

                if (mrzKeyValueMap.get("documentType") != null) {
                    ((EditText) findViewById(R.id.documentType)).setText(mrzKeyValueMap.get("documentType"));
                }

                if (mrzKeyValueMap.get("issuingCountry") != null) {
                    ((EditText) findViewById(R.id.issuingCountry)).setText(mrzKeyValueMap.get("issuingCountry"));
                }

                if (mrzKeyValueMap.get("cardNumber") != null) {
                    ((EditText) findViewById(R.id.cardNumber)).setText(mrzKeyValueMap.get("cardNumber"));
                }

                if (mrzKeyValueMap.get("transactionCode") != null) {
                    ((EditText) findViewById(R.id.transactionCode)).setText(mrzKeyValueMap.get("transactionCode"));
                }

                if (mrzKeyValueMap.get("dateOfBirth") != null) {
                    ((EditText) findViewById(R.id.dateOfBirth)).setText(mrzKeyValueMap.get("dateOfBirth"));
                }

                if (mrzKeyValueMap.get("gender") != null) {
                    ((EditText) findViewById(R.id.gender)).setText(mrzKeyValueMap.get("gender"));
                }

                if (mrzKeyValueMap.get("expirationDate") != null) {
                    ((EditText) findViewById(R.id.expirationDate)).setText(mrzKeyValueMap.get("expirationDate"));
                }

                if (mrzKeyValueMap.get("nationality") != null) {
                    ((EditText) findViewById(R.id.nationality)).setText(mrzKeyValueMap.get("nationality"));
                }

                if (mrzKeyValueMap.get("bearerName") != null) {
                    ((EditText) findViewById(R.id.bearerName)).setText(mrzKeyValueMap.get("bearerName"));
                }
            }
        });
    }




    /**
     *  Removes all the current inflated views before displaying the new one (Passport or ID)
     */
    private void removeAllInflatedViews() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                LinearLayout inflaterLayout = (LinearLayout) findViewById(R.id.documentTypeInflaterLayout);
                inflaterLayout.removeAllViews();
            }

        });
    }


    /**
     *
     */
    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "+++ onPause() +++");

        super.onPause();

    }


    /**
     *
     */
    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "+++ onDestroy() +++");

        super.onDestroy();

    }



}
