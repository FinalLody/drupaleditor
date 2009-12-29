package ch.dissem.android.drupal;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import ch.dissem.android.drupal.model.Post;
import ch.dissem.android.drupal.model.UsersBlog;
import ch.dissem.android.drupal.model.WDAO;

public class RecentEntries extends ListActivity {
	private String blogid;
	private ArrayList<UsersBlog> siteList;
	private WDAO wdao;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recent_entries);

		siteList = getIntent().getParcelableArrayListExtra(Main.KEY_SITE_LIST);

		registerForContextMenu(getListView());

		blogid = getIntent().getStringExtra(EditPost.KEY_BLOG_ID);
		wdao = new WDAO(this);
		fillSiteSpinner();
	}

	@Override
	protected void onResume() {
		loadRecentEntries();
		super.onResume();
	}

	protected void loadRecentEntries() {
		final Handler handler = new Handler();
		setProgressBarIndeterminateVisibility(true);
		new Thread() {
			public void run() {
				final Post[] posts = wdao.getPosts(blogid);

				handler.post(new Runnable() {
					public void run() {
						ListView list = getListView();
						list.setAdapter(new PostAdapter(RecentEntries.this,
								posts));

						RecentEntries.this
								.setProgressBarIndeterminateVisibility(false);
					}
				});
			}
		}.start();
	}

	protected void fillSiteSpinner() {
		final Handler handler = new Handler();
		new Thread() {
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						Spinner blogs = (Spinner) findViewById(R.id.sites);
						ArrayAdapter<UsersBlog> adapter = new ArrayAdapter<UsersBlog>(
								RecentEntries.this,
								android.R.layout.simple_spinner_item, siteList);
						adapter
								.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						blogs.setAdapter(adapter);
						blogs.setClickable(true);
						blogs.setSelection(getIntent().getIntExtra(
								Main.KEY_SITE_LIST_SELECTION, 0));
						blogs
								.setOnItemSelectedListener(new SiteSelectedListener());
					}
				});
			}
		}.start();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, R.string.edit, 0, R.string.edit);
		menu.add(Menu.NONE, R.string.delete, 1, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info;
		try {
			info = (AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			Log.e("ctxMenu", "bad menuInfo", e);
			return false;
		}
		final Post post = (Post) getListView().getAdapter().getItem(
				info.position);
		wdao.setCategories(post);

		switch (item.getItemId()) {
		case R.string.edit:
			Intent intentEdit = new Intent(this, EditPost.class);
			intentEdit.putExtra(EditPost.KEY_POST, post);
			intentEdit.putExtra(EditPost.KEY_BLOG_ID, blogid);
			startActivity(intentEdit);
			break;
		case R.string.delete:
			deletePost(post);
			break;
		default:
			return false;
		}
		return true;
	}

	protected void deletePost(final Post post) {
		final Handler handler = new Handler();
		setProgressBarIndeterminateVisibility(true);
		new Thread() {
			public void run() {
				if (wdao.delete(post))
					handler.post(new Runnable() {
						public void run() {
							loadRecentEntries();
						}
					});
				else
					handler.post(new Runnable() {
						public void run() {
							RecentEntries.this
									.setProgressBarIndeterminateVisibility(false);
						}
					});
			}
		}.start();
	}

	protected class SiteSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			UsersBlog site = (UsersBlog) parent.getSelectedItem();
			blogid = site.getBlogid();
			loadRecentEntries();
		}

		public void onNothingSelected(AdapterView<?> parent) {
		}
	}
}
