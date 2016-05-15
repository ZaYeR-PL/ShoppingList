package pl.zayer.shoppinglist.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import pl.zayer.shoppinglist.R;
import pl.zayer.shoppinglist.contentprovideraccess.ContentProviderAccess;
import pl.zayer.shoppinglist.pojos.ShoppingList;
import pl.zayer.shoppinglist.contentprovideraccess.DeleteCallback;
import pl.zayer.shoppinglist.contentprovideraccess.UpdateCallback;

/**
 * Activity class showing shopping list details and enabling editing if shopping list is not
 * archived.
 */
public class ListDetailsActivity extends AppCompatActivity {

    private static final String LOG_TAG = ListDetailsActivity.class.getSimpleName();

    private static final String STATE_NEW_MODE = "state_new_mode";
    private static final String STATE_LIST_ARCHIVED = "state_list_archived";

    public static final String ARG_SHOPPING_LIST = "arg_shopping_list";

    //constants for return result
    public static final String RESULT_OPERATION = "result_operation";
    public static final int OPERATION_ADD = 10;
    public static final int OPERATION_UPDATE = 20;
    public static final int OPERATION_REMOVE = 30;
    public static final String RESULT_SHOPPING_LIST = "result_shopping_list";

    private static final String TAG_FRAGMENT_LIST_DETAILS = "tag_fragment_list_details";

    private CoordinatorLayout coordinatorCL;

    private ListDetailsActivityFragment listDetailsFragment;
    private boolean newMode;
    private boolean listArchived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        coordinatorCL = (CoordinatorLayout) findViewById(R.id.coordinator_cl);

        if (getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_LIST_DETAILS) != null) {
            listDetailsFragment = (ListDetailsActivityFragment)
                    getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_LIST_DETAILS);
        } else {
            Bundle args = getIntent().getExtras();
            if (args != null && args.containsKey(ARG_SHOPPING_LIST)) {
                //if shopping list is provided in activity args, then enable edit mode
                ShoppingList initialShoppingList = args.getParcelable(ARG_SHOPPING_LIST);
                listArchived = initialShoppingList.isArchived();
                listDetailsFragment = ListDetailsActivityFragment.newInstance(initialShoppingList);
                newMode = false;
            } else {
                //else enable create new mode
                listDetailsFragment = ListDetailsActivityFragment.newInstance();
                listArchived = false;
                newMode = true;
            }
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.container,
                listDetailsFragment,
                TAG_FRAGMENT_LIST_DETAILS)
                .commit();
    }

    /**
     * Menu options are only available if shopping list is NOT archived.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (!listArchived) {
            getMenuInflater().inflate(R.menu.menu_list_details, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_archive) {
            archiveShoppingList();
            return true;
        } else if (id == R.id.action_delete) {
            deleteShoppingList();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates shopping list in database with archived state, returns result extras and exits
     * activity.
     */
    private void archiveShoppingList() {
        if (listDetailsFragment == null) {
            Log.e(LOG_TAG, "List Details Fragment is null!");
            return;
        }
        listDetailsFragment.showViewLoading();

        ContentProviderAccess cpa = new ContentProviderAccess(getContentResolver());
        ShoppingList currentShoppingList = listDetailsFragment.getCurrentShoppingList();
        currentShoppingList.setArchived(true);

        UpdateCallback<ShoppingList> callback = new UpdateCallback<ShoppingList>() {
            @Override
            public void onSuccess(ShoppingList object) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra(RESULT_OPERATION, OPERATION_REMOVE);
                returnIntent.putExtra(RESULT_SHOPPING_LIST, listDetailsFragment.getCurrentShoppingList());
                setResult(RESULT_OK, returnIntent);
                finish();
            }

            @Override
            public void onFailure(int errorCode) {
                Snackbar.make(coordinatorCL, R.string.fragment_list_details_error_update_shopping_list, Snackbar.LENGTH_LONG)
                        .setAction(R.string.snackbar_action_retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                archiveShoppingList();
                            }
                        }).show();
            }
        };
        cpa.updateShoppingList(currentShoppingList, callback);
    }

    /**
     * Deletes shopping list in database, returns result extras and exits activity.
     */
    private void deleteShoppingList() {
        if (listDetailsFragment == null) {
            Log.e(LOG_TAG, "List Details Fragment is null!");
            return;
        }
        listDetailsFragment.showViewLoading();

        ContentProviderAccess cpa = new ContentProviderAccess(getContentResolver());
        ShoppingList currentShoppingList = listDetailsFragment.getCurrentShoppingList();

        DeleteCallback<ShoppingList> callback = new DeleteCallback<ShoppingList>() {
            @Override
            public void onSuccess(ShoppingList object) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra(RESULT_OPERATION, OPERATION_REMOVE);
                returnIntent.putExtra(RESULT_SHOPPING_LIST, listDetailsFragment.getCurrentShoppingList());
                setResult(RESULT_OK, returnIntent);
                finish();
            }

            @Override
            public void onFailure(int errorCode) {
                Snackbar.make(coordinatorCL, R.string.fragment_list_details_error_delete_shopping_list, Snackbar.LENGTH_LONG)
                        .setAction(R.string.snackbar_action_retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                deleteShoppingList();
                            }
                        }).show();
            }
        };
        cpa.deleteShoppingList(currentShoppingList, callback);
    }

    /**
     * Makes sure no EditTexts are focused, then waits for queries to database in fragment to finish
     * (queries are called then EditText looses focus) and calls returnResult().
     * Called by back button press(called also by back arrow in ActionBar press).
     */
    @Override
    public void onBackPressed() {
        Log.i(LOG_TAG, "onBackPressed()");

        if (listDetailsFragment != null) {
            listDetailsFragment.showViewLoading();
            coordinatorCL.requestFocus();
            if (!listDetailsFragment.isQueryRunning()) {
                returnResult();
            } else {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (listDetailsFragment.isQueryRunning()) {
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        returnResult();
                    }
                });
                thread.start();
            }
        }
        //super.onBackPressed();
    }

    /**
     * Puts updated shopping list and type of operation to perform in result extras and exits
     * activity.
     */
    private void returnResult() {
        Log.i(LOG_TAG, "returnResult()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent returnIntent = new Intent();
                if (!newMode) {
                    returnIntent.putExtra(RESULT_OPERATION, OPERATION_UPDATE);
                } else {
                    returnIntent.putExtra(RESULT_OPERATION, OPERATION_ADD);
                }
                returnIntent.putExtra(RESULT_SHOPPING_LIST, listDetailsFragment.getCurrentShoppingList());
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        newMode = savedInstanceState.getBoolean(STATE_NEW_MODE);
        listArchived = savedInstanceState.getBoolean(STATE_LIST_ARCHIVED);
        invalidateOptionsMenu();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_NEW_MODE, newMode);
        outState.putBoolean(STATE_LIST_ARCHIVED, listArchived);
        super.onSaveInstanceState(outState);
    }
}