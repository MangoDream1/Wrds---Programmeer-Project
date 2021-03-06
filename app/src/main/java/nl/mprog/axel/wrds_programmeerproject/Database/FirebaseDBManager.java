package nl.mprog.axel.wrds_programmeerproject.Database;

import android.database.Cursor;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nl.mprog.axel.wrds_programmeerproject.Interfaces.FirebaseKeyInterface;
import nl.mprog.axel.wrds_programmeerproject.Interfaces.QueryFirebaseInterface;

/**
 * Created by axel on 24-1-17.
 *
 * FirebaseDBManager handles all Firebase database functions. Query, delete, update and insert.
 */

public class FirebaseDBManager {

    private static FirebaseDBManager instance = null;
    private static DatabaseManager dbm;
    private static FirebaseDatabase firebaseDB;

    private FirebaseDBManager() {
        // Only exists to defeat instantiation
    }

    /**
     * If instance exists return instance else create new and return
     * @return FirebaseDBManager instance
     */
    public static FirebaseDBManager getInstance() {
        if (instance == null) {
            dbm = DatabaseManager.getInstance();
            firebaseDB = FirebaseDatabase.getInstance();
            instance = new FirebaseDBManager();
        }

        return instance;
    }

    /**
     * Create user.
     *
     * Save username in usernames and set value user id and save user id in users
     * and save username
     * @param username  username
     * @param id        user id
     */
    public void createUser(String username, String id) {
        firebaseDB.getReference().child("users").child(id).setValue(username);
        firebaseDB.getReference().child("usernames").child(username).setValue(id);
    }

    /**
     * Checks if firebaseId still exists in Database
     * @param firebaseId    firebaseId
     * @param callback      callback function
     */
    public void listIdExists(final String firebaseId, final Object callback) {
        firebaseDB.getReference().child("lists").child(firebaseId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        ((FirebaseKeyInterface) callback)
                                .keyStillExists(firebaseId, !dataSnapshot.hasChild("deletedOn"));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    /**
     * Gets list from Firebase. Uses QueryFirebaseInterface for callbacks.
     * @param key key
     * @param callback callback
     */
    public void getListFirebase(final String key, final Object callback) {
        firebaseDB.getReference().child("lists").child(key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                        ((QueryFirebaseInterface) callback).onDataChange(data, key);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        ((QueryFirebaseInterface) callback).onCancelled();
                    }
                });
    }

    /**
     * Upload list
     *
     * Creates key or finds local key and starts uploadList(final String key,
     * final long listId, String userId)
     * @param listId    local list id
     * @param userId    user id
     * @return          firebase key location
     */
    public String uploadList(long listId, String userId) {
        String key = dbm.getFirebaseId(listId);

        if (key == null) {
            key = firebaseDB.getReference().child("lists").push().getKey();
            dbm.updateFirebaseId(listId, key);
        }

        if (dbm.isListOwner(listId)) {
            uploadList(key, listId, userId);
        }

        return key;
    }

    /**
     * Upload list
     *
     * Finds username and starts uploadList(long listId, String key, String username)
     * @param key       firebase list key
     * @param listId    local list id
     * @param userId    user id
     */
    private void uploadList(final String key, final long listId, String userId) {
        firebaseDB.getReference().child("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            uploadList(listId, key, dataSnapshot.getValue(String.class));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    /**
     * Upload list
     *
     * Adds list to lists in Firebase with new ke and adds username
     * @param listId    local list id
     * @param key       firebase key
     * @param username  username
     */
    private void uploadList(long listId, String key, String username) {
        HashMap<String, Object> data = createListHashTable(listId, username);
        firebaseDB.getReference().child("lists").child(key).setValue(data);
    }

    /**
     * Delete list in firebase
     *
     * Removes list data and adds deletion date. Also delete firebase id in local database
     * @param listId        local list id
     * @param firebaseId    firebase id
     */
    public void deleteList(long listId, String firebaseId) {
        dbm.updateFirebaseId(listId, null);

        firebaseDB.getReference().child("lists").child(firebaseId).removeValue();

        // Keep id in list in the case it gets reused and others will share wrong lists
        // Save date deletion so that old lists can be deleted at a later stage
        firebaseDB.getReference().child("lists").child(firebaseId)
                .child("deletedOn").setValue(ServerValue.TIMESTAMP);

    }

    /**
     * Create HashMap with list data
     * @param listId    local list id
     * @param username  username
     * @return          HashMap with list data
     */
    private HashMap<String, Object> createListHashTable(long listId, String username) {
        Cursor lCursor = dbm.getSingleList(listId);

        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("title", lCursor.getString(lCursor.getColumnIndex(DatabaseHelper.STR_TITLE)));
        hashMap.put("desc", lCursor.getString(lCursor.getColumnIndex(DatabaseHelper.STR_DESC)));
        hashMap.put("createdAt", lCursor.getString(
                lCursor.getColumnIndex(DatabaseHelper.DT_CREATED_AT)));
        hashMap.put("languageA", lCursor.getString(
                lCursor.getColumnIndex(DatabaseHelper.STR_LANGUAGE_A)));
        hashMap.put("languageB", lCursor.getString(
                lCursor.getColumnIndex(DatabaseHelper.STR_LANGUAGE_B)));
        hashMap.put("username", username);

        return addWordsHashMap(listId, hashMap);
    }

    /**
     * Adds list words to HashMap
     * @param listId    list id
     * @param hashMap   hashMap without words
     * @return          hashMap with words
     */
    private HashMap<String, Object> addWordsHashMap(long listId, HashMap<String, Object> hashMap) {
        Cursor wCursor = dbm.getListWords(listId);

        ArrayList<HashMap<String, String>> wordList = new ArrayList<>();

        for (wCursor.moveToFirst(); !wCursor.isAfterLast(); wCursor.moveToNext()) {
            HashMap<String, String> word = new HashMap<>();

            word.put("wordA", wCursor.getString(wCursor.getColumnIndex(DatabaseHelper.STR_WORD_A)));
            word.put("wordB", wCursor.getString(wCursor.getColumnIndex(DatabaseHelper.STR_WORD_B)));

            wordList.add(word);
        }

        hashMap.put("words", wordList);

        return hashMap;
    }

}
