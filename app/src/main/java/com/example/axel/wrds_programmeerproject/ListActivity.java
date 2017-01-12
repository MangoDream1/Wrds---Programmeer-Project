package com.example.axel.wrds_programmeerproject;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class ListActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener {

    private long listId;
    private DatabaseManager dbm;
    private WordsCursorAdapter adapter;

    private Button addWordButton;
    private EditText wordAEditText;
    private EditText wordBEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        Intent intent = getIntent();
        listId = intent.getLongExtra("id", 0L);

        dbm = DatabaseManager.getInstance();

        Cursor listCursor = dbm.getUserListTitle(listId);
        String title = listCursor.getString(listCursor.getColumnIndex(DatabaseHelper.str_title));
        String lanA  = listCursor.getString(listCursor.getColumnIndex(DatabaseHelper.str_languageA));
        String lanB  = listCursor.getString(listCursor.getColumnIndex(DatabaseHelper.str_languageB));

        title = String.format("%s (%s - %s)", title, lanA, lanB);

        // Creates toolbar and sets title
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_menu);
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);

        Cursor cursor = dbm.getListWords(listId);

        adapter = new WordsCursorAdapter(this, R.layout.word_item, cursor, 0);

        // Populate listView
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        // Set editText footer
        View footer = getLayoutInflater().inflate(R.layout.word_footer, null);
        listView.addFooterView(footer);

        // Set onClick
        addWordButton = (Button) footer.findViewById(R.id.add_word_button);
        addWordButton.setOnClickListener(this);

        // Find editTexts
        wordAEditText = (EditText) findViewById(R.id.wordA_editText);
        wordBEditText = (EditText) findViewById(R.id.wordB_editText);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Create menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        menu.findItem(R.id.play).setVisible(true);

        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_word_button:
                String wordA = wordAEditText.getText().toString();
                String wordB = wordBEditText.getText().toString();

                dbm.insertWord(listId, wordA, wordB);
                dataChange();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        // TODO REPLACE WITH SOMETHING ELSE HERE
        dbm.deleteWord(id);
        dataChange();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.play:
                Intent intent = new Intent(this, ExamActivity.class);
                intent.putExtra("listId", listId);

                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void dataChange() {
        adapter.swapCursor(dbm.getListWords(listId));
        adapter.notifyDataSetChanged();
    }


}