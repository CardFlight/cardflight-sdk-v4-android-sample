package com.cardflight.sdk.sample;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cardflight.sdk.Amount;
import com.cardflight.sdk.CardAID;
import com.cardflight.sdk.CardInfo;
import com.cardflight.sdk.CardReaderInfo;
import com.cardflight.sdk.Credentials;
import com.cardflight.sdk.HistoricalTransaction;
import com.cardflight.sdk.Message;
import com.cardflight.sdk.Transaction;
import com.cardflight.sdk.TransactionParameters;
import com.cardflight.sdk.enumerations.CVM;
import com.cardflight.sdk.enumerations.CardInputMethod;
import com.cardflight.sdk.enumerations.CardReaderEvent;
import com.cardflight.sdk.enumerations.KeyedEntryEvent;
import com.cardflight.sdk.enumerations.ProcessOption;
import com.cardflight.sdk.enumerations.TransactionResult;
import com.cardflight.sdk.enumerations.TransactionState;
import com.cardflight.sdk.interfaces.CompletionHandler;
import com.cardflight.sdk.interfaces.TransactionHandler;
import com.cardflight.sdk.utils.Logger;
import com.github.gcacace.signaturepad.views.SignaturePad;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements
        ReadersAdapter.CardReaderInfoClickListener, View.OnClickListener, TransactionHandler {

    private static final String API_KEY = "YOUR API KEY";
    private static final String ACCOUNT_TOKEN = "YOUR ACCOUNT TOKEN";

    private String TAG = getClass().getSimpleName();
    private TextView mTransactionStateMessage;
    private TextView mDisplayMessage;
    private Button mStartButton;
    private TextView mReaderMessage;
    private ReadersAdapter mReaderAdapter;
    private Transaction mTransaction;
    private TransactionState mCurrentTransactionState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH, Manifest.permission.INTERNET,
                Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            startActivity(new Intent(this, IntroActivity.class));
        }
        /*
         * Set logging mode of the SDK
         * Pass true to enable developer logging mode to the console
         */
        Logger.setIsLogging(true);
        setupLayout();

        /*
         * This is a newly constructed transaction object
         * Callbacks will be received immediately
         */
        mTransaction = new Transaction(this, this);
    }

    private void setupLayout() {
        mTransactionStateMessage = findViewById(R.id.transactionStateMessage);
        mDisplayMessage = findViewById(R.id.displayMessage);
        mStartButton = findViewById(R.id.startButton);
        mReaderMessage = findViewById(R.id.readerMessage);
        mReaderAdapter = new ReadersAdapter(this);
        RecyclerView recyclerView = findViewById(R.id.recyclerview_readers);
        recyclerView.setAdapter(mReaderAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        TextView version = findViewById(R.id.versionText);

        /*
         * Sets text view at bottom right to current version of CardFlight SDK
         */
        String formattedVersion = String.format(Locale.US, "%s: %s",
                getString(R.string.title_sdk_version),
                com.cardflight.sdk.BuildConfig.VERSION_NAME);
        version.setText(formattedVersion);

        mStartButton.setOnClickListener(this);
    }

    private void createCredentials() {
        /*
         * An object created to authenticate a Transaction object
         */
        final Credentials newCredentials = new Credentials();

        /*
         * Assign an API Key and Account Token to the Credentials object
         * When called, a network request is made to validate the parameters provided with the
         * CardFlight Gateway
         */
        newCredentials.setup(API_KEY, ACCOUNT_TOKEN, new CompletionHandler() {
            @Override
            public void onCompleted(boolean isSuccess, @Nullable Error error) {

                if (isSuccess) {
                    TransactionParameters transactionParams = createTransactionParameters(
                            newCredentials);

                    /*
                     * When a Transaction is in state PENDING TRANSACTION PARAMETERS, call Begin
                     * Sale or Begin Authorization with a valid Transaction Parameters object to
                     * begin a new sale or authorization
                     */
                    mTransaction.beginSale(transactionParams);

                    /*
                     * Call Scan Bluetooth Card Readers to discover available Bluetooth Readers
                     */
                    mTransaction.scanBluetoothCardReaders();

                } else {
                    if (error != null) {

                        /*
                         * If network request to validate the parameters fails, display an alert
                         * with error message if one exists
                         */
                        String errorMessage = String.format(Locale.US, "%s %s",
                                "Please check that credentials are valid.\n\nError:",
                                error.getMessage());

                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Error")
                                .setMessage(errorMessage)
                                .setPositiveButton(android.R.string.ok, null)
                                .setCancelable(false)
                                .create()
                                .show();
                    }
                }
            }
        });
    }

    private TransactionParameters createTransactionParameters(Credentials credentials) {
        /*
         * Set amount of transaction using Bankers rounding
         */
        final Amount saleAmount = new Amount(
                new BigDecimal(1.00).setScale(2, BigDecimal.ROUND_HALF_EVEN));

        /*
         * Example of a metadata hash
         */
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Key", "Value");

        /*
         * Example of a callback url
         */
        URL callbackURL = null;
        try {
            callbackURL = new URL("https://fake.callback.url.com/");
        } catch (MalformedURLException ignored) {
        }

        /*
         * Setup Transaction Parameters
         */
        TransactionParameters transactionParams = new TransactionParameters.Builder(
                saleAmount, credentials)
                /*
                 * If a callback url is specified, the CardFlight Gateway will provide that url
                 * with all transaction details
                 */
                .setCallbackUrl(callbackURL)

                /*
                 * The boolean for requesting a signature is only honored for keyed and swiped
                 * transactions
                 * The CardFlight Gateway decides whether to request a signature for all other card
                 * input methods used
                 */
                .setRequireSignature(true)

                /*
                 * The metadata hash is used to store any additional information with a
                 * Transaction on the CardFlight Gateway
                 * This data will also be sent to the callback url if one was provided
                 */
                .setMetadata(metadata)
                .build();

        return transactionParams;
    }

    private void resetTransaction() {
        mTransaction.close(this);
        mDisplayMessage.setText(getString(R.string.display_message));
        mReaderMessage.setText(getString(R.string.reader_event_message));
        mTransaction = new Transaction(this, this);
    }

    /***********************************************************************************************
     * ReadersAdapter.CardReaderInfoClickListener implementation
     **********************************************************************************************/

    @Override
    public void onCardReaderInfoClicked(CardReaderInfo cardReaderInfo) {
        /*
         * Call Select Card Reader on the Transaction with a [Card Reader Info object]
         */
        mTransaction.selectCardReader(cardReaderInfo, null);
    }

    /***********************************************************************************************
     * View.OnClickListener implementation
     **********************************************************************************************/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startButton:
                if (mCurrentTransactionState == TransactionState.COMPLETED) {
                    resetTransaction();
                }

                createCredentials();
                break;
        }
    }

    /***********************************************************************************************
     * TransactionHandler implementation
     **********************************************************************************************/

    /**
     * Current state of the transaction
     * Most transactions will proceed through these states in a linear order
     * States: UNKNOWN, PENDING_TRANSACTION_PARAMETERS, PENDING_CARD_INPUT,
     * PENDING_PROCESS_OPTION, PROCESSING, COMPLETED, DEFERRED
     */
    @Override
    public void onTransactionStateUpdate(TransactionState transactionState, @Nullable Error error) {
        mCurrentTransactionState = transactionState;

        String message = String.format(Locale.US, "%s %s",
                getString(R.string.transaction_state_message), transactionState.name());
        mTransactionStateMessage.setText(message);

        switch (transactionState) {
            case PENDING_PROCESS_OPTION:
                mStartButton.setEnabled(false);
                mStartButton.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.disabled_button_color));
                break;
            case COMPLETED:
                mStartButton.setEnabled(true);
                mStartButton.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.cardflight_blue));
                break;
        }
    }

    @Override
    public void onUpdateCardReaders(CardReaderInfo[] cardReaderInfoArray) {
        /*
         * When reader availability changes, the Transaction Handler will return the
         * [onUpdateCardReaders] callback with an array of available Card Reader Info objects
         */
        mReaderAdapter.updateReaderList(cardReaderInfoArray);
    }

    /**
     * Method by which card info was captured: UNKNOWN, KEY, SWIPE, DIP, TAP, SWIPE_FALLBACK,
     * QUICK_CHIP
     */
    @Override
    public void onUpdateCardInputMethods(CardInputMethod[] cardInputMethods) {
        if (cardInputMethods != null) {
            String message = String.format(Locale.US, "%s %s", getString(R.string.display_message),
                    Arrays.toString(cardInputMethods));
            mDisplayMessage.setText(message);
        }
    }

    /**
     * Events triggered by the card reader: DISCONNECTED, CONNECTED, CONNECTING,
     * CONNECTION_ERRORED, CARD_SWIPED,
     * CARD_SWIPE_ERRORED, CARD_DIPPED, CARD_DIP_ERRORED, CARD_REMOVED, CARD_TAPPED,
     * CARD_TAP_ERRORED, UPDATE_STARTED, UPDATE_COMPLETED, BLUETOOTH_PERMISSION_NOT_GRANTED,
     * BLUETOOTH_ADMIN_PERMISSION_NOT_GRANTED, RECORD_AUDIO_PERMISSION_NOT_GRANTED,
     * ACCESS_COARSE_LOCATION_PERMISSION_NOT_GRANTED,
     * WRITE_EXTERNAL_STORAGE_PERMISSION_NOT_GRANTED, READER_FATAL_ERROR, BATTERY_STATUS_UPDATED
     */
    @Override
    public void onReceiveCardReaderEvent(CardReaderInfo cardReaderInfo,
            CardReaderEvent cardReaderEvent) {
        String message = String.format(Locale.US, "%s %s", getString(R.string.reader_event_message),
                cardReaderEvent.name());
        mReaderMessage.setText(message);
    }

    @Override
    public void onReceiveKeyedEntryEvent(KeyedEntryEvent keyedEntryEvent) {

    }

    /**
     * The Historical Transaction object is created to represent the record of a completed
     * Transaction
     * Void and Refund can be performed against the Historical Transaction returned on an
     * APPROVED SALE Transaction
     */
    @Override
    public void onCompleted(HistoricalTransaction historicalTransaction) {
        if (historicalTransaction.getResult() == TransactionResult.APPROVED) {

            /*
             * If transaction result is approved, void transaction
             * The transactions in this starter app is currently being conducted on a production
             * environment.
             * Without voiding, you would be charged $1.00 after each successful transaction
             */
            historicalTransaction.voidTransaction(new CompletionHandler() {
                @Override
                public void onCompleted(boolean isSuccess, @Nullable Error error) {
                    if (isSuccess) {
                        Log.d(TAG, "Successfully voided mTransaction.");
                    } else {
                        Log.d(TAG, "Failed to void mTransaction");
                        if (error != null) {
                            String errorMessage = String.format(Locale.US, "%s %s",
                                    "Error Voiding Transaction",
                                    error.getMessage());
                            Log.e(TAG, errorMessage);
                        }
                    }
                }
            });
            mTransaction.close(this);
        }
    }

    /**
     * The Defer Transaction callback is fired if the user opts to defer the transaction,
     * which generally would be done if the user did not have internet connectivity to process
     * the transaction.
     * A deferred transaction is in an incomplete state and must be resumed manually.
     */
    @Override
    public void onTransactionDeferred(byte[] bytes) {

    }

    @Override
    public void onRequestDisplayMessage(String s) {

    }

    @Override
    public void onRequestDisplayMessage(Message message) {
        /*
         * Primary message: High priority message, should be displayed to the user
         */
        String displayMessage = String.format(Locale.US, "%s %s ",
                getString(R.string.display_message), message.getPrimary());

        if (message.getSecondary() != null) {
            /*
             * Secondary message: Lower priority message, should be displayed to the user
             */
            displayMessage = String.format(Locale.US, "%s, %s", displayMessage,
                    message.getSecondary());
        }

        mDisplayMessage.setText(displayMessage);
    }

    /**
     * When a Transaction is in the state PROCESSING and the Transaction Handler returns the
     * Request CVM callback, call the Select Card AID method with the cardholder selected Card
     * AID object to proceed with processing
     */
    @Override
    public void onRequestCardAidSelection(CardAID[] cardAids) {
        if (cardAids.length > 0) {
            mTransaction.selectCardAid(cardAids[0]);
        }
    }

    /**
     * Transaction requests a Process Option selection of UNKNOWN, PROCESS, ABORT, or DEFER
     */
    @Override
    public void onRequestProcessOption(CardInfo cardInfo) {
        mTransaction.selectProcessOption(ProcessOption.PROCESS);
    }

    /**
     * CVM stands for Cardholder Verification Method, i.e. the cardholder's signature
     * Can request no CVM or a Signature CVM
     * When Request CVM callback is requested by the Transaction Handler, the cardholder
     * should be prompted to provide signature verification
     * Call Attach CVM with the raw cardholder verification data collected from the cardholder
     */
    @Override
    public void onRequestCvm(CVM cvm) {

        final View view = LayoutInflater.from(this).inflate(R.layout.fragment_signature,
                null,
                false);

        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle(
                "Cardholder Verification Requested").setView(view).create();

        ((SignaturePad) view.findViewById(R.id.signature_pad)).setOnSignedListener(
                new SignaturePad.OnSignedListener() {
                    @Override
                    public void onStartSigning() {

                    }

                    @Override
                    public void onSigned() {
                        Bitmap bitmap = ((SignaturePad) view.findViewById(R.id.signature_pad))
                                .getSignatureBitmap();

                        mTransaction.attachSignature(bitmap);
                        dialog.dismiss();
                    }

                    @Override
                    public void onClear() {

                    }
                });

        dialog.show();
    }
}
