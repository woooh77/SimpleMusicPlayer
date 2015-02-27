package com.mp.simplemusicplayer;

import android.app.Activity;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.os.Bundle;
import android.view.Menu;
import android.webkit.WebViewFragment;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.MediaController.MediaPlayerControl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.mp.simplemusicplayer.MusicService.MusicBinder;

import android.net.Uri;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.database.Cursor;
import android.widget.ListView;

public class MainActivity extends Activity implements MediaPlayerControl{

	private boolean paused=false, playbackPaused=false;
	private ArrayList<Song> songList;
	private ListView songView;
	private MusicService musicSrv;
	private Intent playIntent;
	private boolean musicBound=false;
	private MusicController controller;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private ActionBarDrawerToggle mDrawerToggle;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Music");
        setContentView(R.layout.activity_main);

      songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        getSongList();
        Collections.sort(songList, new Comparator<Song>(){
        	  public int compare(Song a, Song b){
        	    return a.getTitle().compareTo(b.getTitle());
        	  }
        	});
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        setController();
        }

   	public void setTitle(CharSequence title) {
   	    mTitle = title;
   	    getActionBar().setTitle(mTitle);
   	}
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
  //      mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
  //      mDrawerToggle.onConfigurationChanged(newConfig);
    }

  //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){
     
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        MusicBinder binder = (MusicBinder)service;
        //get service
        musicSrv = binder.getService();
        //pass list
        musicSrv.setList(songList);
        musicBound = true;
      }
     
      @Override
      public void onServiceDisconnected(ComponentName name) {
        musicBound = false;
      }
    };
    
    private void setController(){
    	  //set the controller up
    	controller = new MusicController(this);
    	controller.setPrevNextListeners(new View.OnClickListener() {
    		  @Override
    		  public void onClick(View v) {
    		    playNext();
    		  }
    		}, new View.OnClickListener() {
    		  @Override
    		  public void onClick(View v) {
    		    playPrev();
    		  }
    		});
    	controller.setMediaPlayer(this);
    	controller.setAnchorView(findViewById(R.id.song_list));
    	controller.setEnabled(true);
    }
    
    @Override
    protected void onStart() {
      super.onStart();
      if(playIntent==null){
        playIntent = new Intent(this, MusicService.class);
        bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        startService(playIntent);
      }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
 /*   	if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
          }
  */  	switch (item.getItemId()) {
    	case R.id.action_shuffle:
    	  //case R.id.action_shuffle:
    	  musicSrv.setShuffle();
    	  break;
    	case R.id.action_end:
    	  stopService(playIntent);
    	  musicSrv=null;
    	  System.exit(0);
    	  break;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    public void getSongList() {
    	 ContentResolver musicResolver = getContentResolver();
    	 Uri musicuri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    	 Cursor musicCursor = musicResolver.query(musicuri, null, null, null, null);
    	 if(musicCursor!=null && musicCursor.moveToFirst()){
    		  //get columns
    		  int titleColumn = musicCursor.getColumnIndex
    		    (android.provider.MediaStore.Audio.Media.TITLE);
    		  int idColumn = musicCursor.getColumnIndex
    		    (android.provider.MediaStore.Audio.Media._ID);
    		  int artistColumn = musicCursor.getColumnIndex
    		    (android.provider.MediaStore.Audio.Media.ARTIST);
    		  //add songs to list
    		  do {
    		    long thisId = musicCursor.getLong(idColumn);
    		    String thisTitle = musicCursor.getString(titleColumn);
    		    String thisArtist = musicCursor.getString(artistColumn);
    		    songList.add(new Song(thisId, thisTitle, thisArtist));
    		  }
    		  while (musicCursor.moveToNext());
    		}
    	}
    @Override
    protected void onDestroy() {
      stopService(playIntent);
      musicSrv=null;
      super.onDestroy();
    }
	public void songPicked(View view){
		musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
		  musicSrv.playSong();
		  if(playbackPaused){
		    setController();
		    playbackPaused=false;
		  }
		  controller.show(0);		}

	//play next
	private void playNext(){
	  musicSrv.playNext();
	  if(playbackPaused){
		    setController();
		    playbackPaused=false;
		  }
		  controller.show(0);
	}
	 
	//play previous
	private void playPrev(){
	  musicSrv.playPrev();
	  if(playbackPaused){
		    setController();
		    playbackPaused=false;
		  }
	  controller.show(0);
	}
	@Override
	public boolean canPause() {
	  return true;
	}
	@Override
	public boolean canSeekBackward() {
	  return true;
	}
	 
	@Override
	public boolean canSeekForward() {
	  return true;
	}
	@Override
	public int getCurrentPosition() {
	  if(musicSrv!=null && musicBound && musicSrv.isPng())
	    return musicSrv.getPosn();
	  else return 0;
	}
	@Override
	public int getDuration() {
	  if(musicSrv!=null && musicBound && musicSrv.isPng())
	    return musicSrv.getDur();
	  else return 0;
	}
	@Override
	public boolean isPlaying() {
	  if(musicSrv!=null && musicBound)
	    return musicSrv.isPng();
	  return false;
	}
	
	@Override
	public void pause() {
		playbackPaused=true;
		  musicSrv.pausePlayer();
	}
	 
	@Override
	protected void onPause(){
	  super.onPause();
	  paused=true;
	}
	
	@Override
	protected void onResume(){
	  super.onResume();
	  if(paused){
	    setController();
	    paused=false;
	  }
	}
	@Override
	protected void onStop() {
	  controller.hide();
	  super.onStop();
	}
	
	@Override
	public void seekTo(int pos) {
	  musicSrv.seek(pos);
	}
	 
	@Override
	public void start() {
	  musicSrv.go();
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getBufferPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}
}
