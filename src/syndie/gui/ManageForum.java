package syndie.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.i2p.data.Hash;
import net.i2p.data.PrivateKey;
import net.i2p.data.SessionKey;
import net.i2p.data.SigningPrivateKey;
import net.i2p.data.SigningPublicKey;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import syndie.Constants;
import syndie.data.ArchiveInfo;
import syndie.data.ChannelInfo;
import syndie.data.NymKey;
import syndie.data.ReferenceNode;
import syndie.data.SyndieURI;
import syndie.db.DBClient;
import syndie.db.ManageForumExecutor;
import syndie.db.UI;
import syndie.util.DateTime;
import syndie.util.StringUtil;

/**
 *  Parent is a ViewForumTab
 */
public class ManageForum extends BaseComponent implements Translatable, Themeable {
    private NavigationControl _navControl;
    private BanControl _banControl;
    private BookmarkControl _bookmarkControl;
    private URIControl _uriControl;
    private DataCallback _dataCallback;
    private Composite _parent;
    private SyndieURI _uri;
    private Hash _scope;
    private long _scopeId;
    
    private Composite _root;
    private Button _avatar;
    private Image _avatarImgOrig;
    private Image _avatarImg;
    private List _avatarImgStandard;
    private MenuItem _avatarOther;
    private Menu _avatarMenu;
    private Label _nameLabel;
    private Text _name;
    private Label _tagsLabel;
    private Text _tags;
    private Label _descriptionLabel;
    private Text _description;
    private Label _expirationLabel;
    private Text _expiration;
    private Button _expirationManager;
    private ArrayList _referenceNodeRoots;
    private Group _archiveGroup;
    private Button _archiveSelect;
    private Button _archiveRemoveAll;
    private Group _refGroup;
    private Button _refSelect;
    private Button _refRemoveAll;
    private Group _banGroup;
    private Button _banSelect;
    private Button _banRemoveAll;
    private Group _authGroup;
    private Label _authLabel;
    private Button _authRead;
    private Button _authPost;
    private Button _authManage;
    private Button _authReply;
    private Composite _actions;
    private Button _save;
    private Button _cancel;

    private boolean _initialized;
    private boolean _modified;
    
    private ManageForumAuthRead _manageForumAuthRead;
    private ManageForumAuthPost _manageForumAuthPost;
    private ManageForumAuthManage _manageForumAuthManage;
    private ManageForumAuthReply _manageForumAuthReply;
    
    private List<Hash> _managerHashes;
    private List<Hash> _posterHashes;
    private List<SyndieURI> _pubArchiveURIs;
    private List<SyndieURI> _privArchiveURIs;
    private String _passphrase;
    private String _prompt;
    
    private List<StateListener> _listeners;
    
    private boolean _showActions;
    
    public interface StateListener {
        public void settingsModified(boolean canSave);
    }
    
    public ManageForum(DBClient client, UI ui, ThemeRegistry themes, TranslationRegistry trans, NavigationControl navControl, BanControl banControl, BookmarkControl bookmarkControl, URIControl uriControl, DataCallback callback, Composite parent, SyndieURI uri, boolean showActions) {
        super(client, ui, themes, trans);
        _navControl = navControl;
        _banControl = banControl;
        _bookmarkControl = bookmarkControl;
        _uriControl = uriControl;
        _dataCallback = callback;
        _parent = parent;
        _uri = uri;
        _scope = null;
        _scopeId = -1;
        _initialized = false;
        _showActions = showActions;
        _listeners = new ArrayList();
        _avatarImgStandard = new ArrayList();
        _privArchiveURIs = new ArrayList();
        _pubArchiveURIs = new ArrayList();
        _managerHashes = new ArrayList();
        _posterHashes = new ArrayList();
        _referenceNodeRoots = new ArrayList();
        Hash scope = uri.getScope();
        if (scope == null)
            scope = uri.getHash("scope");
        if (scope != null) {
            //List keys = _client.getNymKeys(scope, Constants.KEY_FUNCTION_MANAGE);
            _scope = scope;
            _scopeId = _client.getChannelId(scope);
        } else {
            _ui.debugMessage("no scope!  creating a new one");
        }
        initComponents();
    }
    public boolean getEditable() { return true; }
    public boolean canShow(Hash scope) {
        return (scope != null) && (_scope != null) && _scope.equals(scope);
    }
    
    public void resized() {
        _ui.debugMessage("resizing forum parent");
        applyTheme(_themeRegistry.getTheme()); 
        _parent.layout(true, true);
    }
    
    Composite getRoot() { return _root; }
    
    private void initComponents() {
        _root = new Composite(_parent, SWT.NONE);
        _root.setLayout(new GridLayout(7, false));
        
        loadOrigAvatar();
        
        _avatar = new Button(_root, SWT.PUSH);
        GridData gd = new GridData(GridData.CENTER, GridData.CENTER, false, false, 1, 2);
        gd.widthHint = 54;
        gd.heightHint = 54;
        _avatar.setLayoutData(gd);
        
        _avatarMenu = new Menu(_avatar);
        _avatar.setMenu(_avatarMenu);

        populateAvatarMenu();

        _avatar.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { _avatarMenu.setVisible(true); }
            public void widgetSelected(SelectionEvent selectionEvent) { _avatarMenu.setVisible(true); }
        });
        
        _nameLabel = new Label(_root, SWT.NONE);
        _nameLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        
        _name = new Text(_root, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL, GridData.FILL, true, false);
        //gd.widthHint = 100;
        _name.setLayoutData(gd);
        _name.addModifyListener(new ModifyListener() { public void modifyText(ModifyEvent evt) { modified(); } });
        
        _tagsLabel = new Label(_root, SWT.NONE);
        _tagsLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        
        _tags = new Text(_root, SWT.BORDER | SWT.SINGLE);
        _tags.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));
        _tags.addModifyListener(new ModifyListener() { public void modifyText(ModifyEvent evt) { modified(); } });
        
        _descriptionLabel = new Label(_root, SWT.NONE);
        _descriptionLabel.setLayoutData(new GridData(GridData.END, GridData.BEGINNING, false, false));
        
        int descWidth = 3;
        _description = new Text(_root, SWT.BORDER | SWT.SINGLE);
        _description.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, descWidth, 1));
        _description.addModifyListener(new ModifyListener() { public void modifyText(ModifyEvent evt) { modified(); } });
        
        Composite expiration = new Composite(_root, SWT.NONE);
        expiration.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));
        expiration.setLayout(new GridLayout(3, false));
        
        _expirationLabel = new Label(expiration, SWT.NONE);
        _expirationLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        
        _expiration = new Text(expiration, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL, GridData.FILL, false, false);
        gd.widthHint = 100;
        _expiration.setLayoutData(gd);
        _expiration.addModifyListener(new ModifyListener() { public void modifyText(ModifyEvent evt) { modified(); } });
        
        _expirationManager = new Button(expiration, SWT.PUSH);
        _expirationManager.addSelectionListener(new FireSelectionListener() { public void fire() { _navControl.view(URIHelper.instance().createExpirationURI(_scope)); } });
        _expirationManager.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        _expirationManager.setImage(ImageUtil.ICON_MANAGEABLEFORUM);
        
        if ( (_scope == null) || (_scopeId < 0) )
            _expirationManager.setEnabled(false);
        
        Composite refRow = new Composite(_root, SWT.NONE);
        refRow.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 7, 1));
        refRow.setLayout(new FillLayout(SWT.HORIZONTAL));
        
        _archiveGroup = new Group(refRow, SWT.SHADOW_ETCHED_IN);
        _archiveGroup.setLayout(new FillLayout(SWT.HORIZONTAL));
        
        _archiveSelect = new Button(_archiveGroup, SWT.PUSH);
        _archiveRemoveAll = new Button(_archiveGroup, SWT.PUSH);
        _archiveSelect.addSelectionListener(new FireSelectionListener() {
            public void fire() { new ManageForumArchives(_client, _ui, _themeRegistry, _translationRegistry, ManageForum.this); }
        });
        _archiveRemoveAll.addSelectionListener(new FireSelectionListener() {
            public void fire() { 
                _pubArchiveURIs.clear();
                _privArchiveURIs.clear();
                redrawArchives();
            }
        });
        
        _refGroup = new Group(refRow, SWT.SHADOW_ETCHED_IN);
        _refGroup.setLayout(new FillLayout(SWT.HORIZONTAL));
        
        _refSelect = new Button(_refGroup, SWT.PUSH);
        _refSelect.addSelectionListener(new FireSelectionListener() {
            public void fire() { new ManageForumReferences(_client, _ui, _themeRegistry, _translationRegistry, _navControl, _banControl, _bookmarkControl, ManageForum.this); }
        });
        _refRemoveAll = new Button(_refGroup, SWT.PUSH);
        _refRemoveAll.addSelectionListener(new FireSelectionListener() {
            public void fire() { removeRefs(); }
        });
        
        _banGroup = new Group(refRow, SWT.SHADOW_ETCHED_IN);
        _banGroup.setLayout(new FillLayout(SWT.HORIZONTAL));
        
        _banSelect = new Button(_banGroup, SWT.PUSH);
        _banSelect.addSelectionListener(new FireSelectionListener() {
            public void fire() { new ManageForumBans(_client, _ui, _themeRegistry, _translationRegistry, ManageForum.this); }
        });
        _banRemoveAll = new Button(_banGroup, SWT.PUSH);
        _banRemoveAll.addSelectionListener(new FireSelectionListener() {
            public void fire() { removeBans(); }
        });
        
        _authGroup = new Group(_root, SWT.SHADOW_ETCHED_IN);
        _authGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 7, 1));
        _authGroup.setLayout(new GridLayout(4, true));

        _authLabel = new Label(_authGroup, SWT.WRAP);
        _authLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 4, 1));

        _authRead = new Button(_authGroup, SWT.PUSH);
        _authRead.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _authRead.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { fire(); }
            public void widgetSelected(SelectionEvent selectionEvent) { fire(); }
            private void fire() { 
                if (_manageForumAuthRead == null)
                    _manageForumAuthRead = new ManageForumAuthRead(_client, _ui, _themeRegistry, _translationRegistry, ManageForum.this);
                _manageForumAuthRead.show();
            }
        });
        _authPost = new Button(_authGroup, SWT.PUSH);
        _authPost.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _authPost.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { fire(); }
            public void widgetSelected(SelectionEvent selectionEvent) { fire(); }
            private void fire() { 
                if (_manageForumAuthPost == null)
                    _manageForumAuthPost = new ManageForumAuthPost(_client, _ui, _themeRegistry, _translationRegistry, ManageForum.this);
                _manageForumAuthPost.show();
            }
        });
        _authManage = new Button(_authGroup, SWT.PUSH);
        _authManage.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _authManage.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { fire(); }
            public void widgetSelected(SelectionEvent selectionEvent) { fire(); }
            private void fire() { 
                if (_manageForumAuthManage == null)
                    _manageForumAuthManage = new ManageForumAuthManage(_client, _ui, _themeRegistry, _translationRegistry, ManageForum.this);
                _manageForumAuthManage.show();
            }
        });
        _authReply = new Button(_authGroup, SWT.PUSH);
        _authReply.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _authReply.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { fire(); }
            public void widgetSelected(SelectionEvent selectionEvent) { fire(); }
            private void fire() { 
                if (_manageForumAuthReply == null)
                    _manageForumAuthReply = new ManageForumAuthReply(_client, _ui, _themeRegistry, _translationRegistry, ManageForum.this);
                _manageForumAuthReply.show();
            }
        });
        
        if (_showActions) {
            _actions = new Composite(_root, SWT.NONE);
            _actions.setLayout(new FillLayout(SWT.HORIZONTAL));
            _actions.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, true, 7, 1));
            _cancel = new Button(_actions, SWT.PUSH);
            _save = new Button(_actions, SWT.PUSH);
            _save.setEnabled(false);
            _cancel.setEnabled(false);
            _save.setImage(ImageUtil.ICON_SYNDICATE_STATUS_OK);
            _cancel.setImage(ImageUtil.ICON_SYNDICATE_STATUS_ERROR);

            _save.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { saveChanges(); }
                public void widgetSelected(SelectionEvent selectionEvent) { saveChanges(); }
            });

            _cancel.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { cancelChanges(); }
                public void widgetSelected(SelectionEvent selectionEvent) { cancelChanges(); }
            });
        }
        
        //ChannelInfo info = _client.getChannel(_scopeId);
        loadData();
        
        _translationRegistry.register(this);
        _themeRegistry.register(this);
    }
    
    public void cancelChanges() { loadData(); }
    
    ChannelInfo getChannelInfo() { return _client.getChannel(_scopeId); }
    
    List<SyndieURI> getPublicArchiveURIs() { return new ArrayList(_pubArchiveURIs); }

    List<SyndieURI> getPrivateArchiveURIs() { return new ArrayList(_privArchiveURIs); }
    
    void setArchives(List<SyndieURI> pubURIs, List<SyndieURI> privURIs) {
        _pubArchiveURIs.clear();
        _pubArchiveURIs.addAll(pubURIs);
        _privArchiveURIs.clear();
        _privArchiveURIs.addAll(privURIs);
        
        modified();
        
        redrawArchives();
    }
    
    public void dispose() {
        ImageUtil.dispose(_avatarImgOrig);
        for (int i = 0; i < _avatarImgStandard.size(); i++)
            ImageUtil.dispose((Image)_avatarImgStandard.get(i));
        _translationRegistry.unregister(this);
        _themeRegistry.unregister(this);
        if (_manageForumAuthRead != null) _manageForumAuthRead.dispose();
        if (_manageForumAuthPost != null) _manageForumAuthPost.dispose();
        if (_manageForumAuthManage != null) _manageForumAuthManage.dispose();
        if (_manageForumAuthReply != null) _manageForumAuthReply.dispose();
    }
    
    /* called when the tab is closed */
    public boolean confirmClose() {
        if (!_modified) return true;
        MessageBox confirm = new MessageBox(_parent.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        confirm.setText(getText("Confirm"));
        confirm.setMessage(getText("Do you want to discard these changes to the forum?"));
        int rc = confirm.open();
        if (rc == SWT.YES) {
            return true;
        } else if (rc == SWT.NO) {
            return false;
        } else {
            return false;
        }
    }    
    
    private void promptForPBE() {
        PassphrasePrompt prompt = new PassphrasePrompt(_client, _ui, _themeRegistry, _translationRegistry, _root.getShell(), true);
        prompt.setPassphraseListener(new PassphrasePrompt.PassphraseListener() {
            public void promptComplete(String passphraseEntered, String promptEntered) {
                _passphrase = passphraseEntered;
                _prompt = promptEntered;
            }
            public void promptAborted() {}
        });
        prompt.open();
    }
    
    public void saveChanges() {
        ManageForumExecutor exec = new ManageForumExecutor(_client, _ui, new ManageForumExecutor.ManageForumState() {
            public boolean getCreateReadKey() {
                if (_manageForumAuthRead != null) {
                    return _manageForumAuthRead.getNewKey() || (_scopeId < 0);
                } else {
                    return false;
                }
            }
            public boolean getCreatePostIdentity() {
                if (_manageForumAuthPost != null) {
                    return _manageForumAuthPost.getNewIdentity();
                } else {
                    return false;
                }
            }
            public boolean getCreateManageIdentity() {
                if (_manageForumAuthManage != null) {
                    return _manageForumAuthManage.getNewIdentity();
                } else {
                    return false;
                }
            }
            public boolean getCreateReplyKey() {
                if (_manageForumAuthReply != null) {
                    return _manageForumAuthReply.getRotate() || (_scopeId < 0);
                } else {
                    return false;
                }
            }
            
            public byte[] getAvatarData() {
                Image avatar = _avatarImg;
                if (avatar != null) {
                    try {
                        return ImageUtil.serializeImage(avatar);
                    } catch (SWTException se) {
                        _ui.errorMessage("Internal error serializing image", se);
                        return null;
                    }
                } else {
                    return null;
                }
            }
        
            public String getName() { return _name.getText(); }
            public String getDescription() { return _description.getText(); }
            public long getLastEdition() {
                if (_scopeId >= 0) 
                    return _client.getChannelVersion(_scopeId);
                else
                    return -1;
            }

            public boolean getAllowPublicPosts() { 
                if (_manageForumAuthPost != null) {
                    return _manageForumAuthPost.getAllowPublicPosts();
                } else {
                    ChannelInfo info = getChannelInfo();
                    if (info != null)
                        return info.getAllowPublicPosts();
                    else
                        return false;
                }
            }
            public boolean getAllowPublicReplies() { 
                if (_manageForumAuthPost != null) {
                    return _manageForumAuthPost.getAllowPublicReplies();
                } else {
                    ChannelInfo info = getChannelInfo();
                    if (info != null)
                        return info.getAllowPublicReplies();
                    else
                        return false;
                }
            }
            public Set<String> getPublicTags() { return Collections.EMPTY_SET; }
            public Set<String> getPrivateTags() { 
                String tags[] = StringUtil.split(" \t\r\n,", _tags.getText(), false);
                Set rv = new HashSet(tags.length);
                for (int i = 0; i < tags.length; i++)
                    rv.add(tags[i]);
                return rv;
            }
            public Set<SigningPublicKey> getAuthorizedPosters() { 
                if (_manageForumAuthPost != null) {
                    return getPubKeys(_manageForumAuthPost.getAuthorizedPosters());
                } else {
                    ChannelInfo info = getChannelInfo();
                    if (info != null)
                        return info.getAuthorizedPosters();
                    else
                        return new HashSet();
                }
            }
            public Set<SigningPublicKey> getAuthorizedManagers() {
                if (_manageForumAuthManage != null) {
                    return getPubKeys(_manageForumAuthManage.getAuthorizedManagers());
                } else {
                    ChannelInfo info = getChannelInfo();
                    if (info != null)
                        return info.getAuthorizedManagers();
                    else
                        return new HashSet();
                }
            }
            private Set<SigningPublicKey> getPubKeys(List<Hash> scopes) {
                Set<SigningPublicKey> rv = new HashSet();
                for (int i = 0; i < scopes.size(); i++) {
                    Hash scope = scopes.get(i);
                    SigningPublicKey key = _client.getChannelIdentKey(scope);
                    if (key != null)
                        rv.add(key);
                }
                return rv;
            }
            public String getReferences() { return (_referenceNodeRoots != null ? ReferenceNode.walk(_referenceNodeRoots) : ""); }
            public Set<SyndieURI> getPublicArchives() { return getArchives(_pubArchiveURIs); }
            public Set<SyndieURI> getPrivateArchives() { return getArchives(_privArchiveURIs); }
            private Set<SyndieURI> getArchives(List uris) {
                Set archives = new HashSet();
                for (Iterator iter = uris.iterator(); iter.hasNext(); ) {
                    SyndieURI uri = (SyndieURI)iter.next();
                    ArchiveInfo info = new ArchiveInfo(uri);
                    archives.add(info);
                    _ui.debugMessage("getArchives: sz=" + archives.size() + " uri=" + uri);
                }
                _ui.debugMessage("getArchives(" + uris + "): found " + archives.size() + " archives");
                return archives;
            }

            public long getChannelId() { return _scopeId; }
            
            public boolean getEncryptContent() { 
                return ((_manageForumAuthRead != null) && (_manageForumAuthRead.getEncryptMetadata()));
            }
            public boolean getPBE() { return false; }
            public String getPassphrase() { return _passphrase; }
            public String getPassphrasePrompt() { return _prompt; }
            /** return the read keys we explicitly want to deliver in the metadata, or null/empty if we don't care */
            public List<SessionKey> getCurrentReadKeys() { 
                if ( (_manageForumAuthRead != null) && (_manageForumAuthRead.getReadKeyPublicRetroactive()) ) {
                    // all session keys we know for reading posts in the forum, whether they were private or public
                    return _client.getReadKeys(_client.getChannelHash(_scopeId), false);
                } else {
                    return null;
                }
            }
            public List<SyndieURI> getCancelledURIs() { return _client.getChannelCancelURIs(_scopeId); }
        });
        exec.execute();
        for (int i = 0; i < _listeners.size(); i++)
            _listeners.get(i).settingsModified(false);
        
        String errs = exec.getErrors();
        if ( (errs != null) && (errs.trim().length() > 0) ) {
            MessageBox box = new MessageBox(_parent.getShell(), SWT.ICON_ERROR | SWT.OK);
            box.setText(getText("Error"));
            box.setMessage(getText("Internal error saving the forum") + ':' + errs);
            box.open();
        } else {
            // ok, now create any of the posts we need to send keys to the right people, 
            // as defined by viewForumAuth*
            
            SyndieURI uri = exec.getForum();
            Hash scope = uri.getScope();
            SessionKey readKey = exec.getCreatedReadKey();
            Hash postIdentity = exec.getCreatedPostIdentity();
            Hash manageIdentity = exec.getCreatedManageIdentity();
            File postFile = null;
            if (postIdentity != null)
                postFile = new File(new File(_client.getOutboundDir(), postIdentity.toBase64()), "meta" + Constants.FILENAME_SUFFIX);
            File manageFile = null;
            if (manageIdentity != null)
                manageFile = new File(new File(_client.getOutboundDir(), manageIdentity.toBase64()), "meta" + Constants.FILENAME_SUFFIX);
            
            _ui.debugMessage("done updating, scope=" + scope + " readKey=" + readKey + " postIdent=" + postIdentity + " manageIdent=" + manageIdentity);
            
            if (_manageForumAuthRead != null) {
                // open a new post to the appropriate locations containing the read key
                if (readKey == null) {
                    // use the existing readkey to send
                    List<NymKey> nks = _client.getNymKeys(uri.getScope(), Constants.KEY_FUNCTION_READ);
                    for (int i = 0; i < nks.size(); i++) {
                        NymKey nk = nks.get(i);
                        if (!nk.getIsExpired()) {
                            readKey = new SessionKey(nk.getData());
                            break;
                        }
                    }
                    if (readKey == null) {
                        // could be attached to the channel and not the nym, so try it there too
                        List<SessionKey> rks = _client.getReadKeys(uri.getScope(), true);
                        for (int i = 0; i < rks.size(); i++) {
                            SessionKey rk = rks.get(i);
                            if (!_client.getChannelReadKeyIsPublic(uri.getScope(), rk)) {
                                readKey = rk;
                                break;
                            }
                        }
                    }
                }
                List<Hash> scopes = _manageForumAuthRead.getSendExplicit();
                for (int i = 0; i < scopes.size(); i++) {
                    Hash to = scopes.get(i);
                    _ui.debugMessage("pop up a window to post the read key to " + to.toBase64());
                    _navControl.view(_uriControl.createPostURI(to, null, true, createReferences(uri.getScope(), readKey), null));
                    //_browser.createPostURI(to, null, true, readKey);
                } 
                if (_manageForumAuthRead.getPostPBE()) {
                    _ui.debugMessage("pop up a window to post to " + _scope.toBase64() + " with pbe prompt [" + _manageForumAuthRead.getSendPassphrasePrompt() + "] for pass [" + _manageForumAuthRead.getSendPassphrase() + "]");
                    _navControl.view(_uriControl.createPostURI(uri.getScope(), null, _manageForumAuthRead.getSendPassphrase(), _manageForumAuthRead.getSendPassphrasePrompt(), createReferences(uri.getScope(), readKey), null));
                    //_browser.createPostURI(_scope, null, false, readKey, _viewForumAuthRead.getSendPassphrase(), _viewForumAuthRead.getSendPassphrasePrompt());
                }
            }
            if ( (postIdentity != null) && (_manageForumAuthPost != null) ) {
                // open a new post to the appropriate locations containing the post identity's keys
                List<Hash> scopes = _manageForumAuthPost.getSendNewExplicit();
                for (int i = 0; i < scopes.size(); i++) {
                    Hash to = scopes.get(i);
                    _ui.debugMessage("pop up a window to post the post identity key to " + to.toBase64());
                    _navControl.view(_uriControl.createPostURI(to, null, true, createReferences(uri.getScope(), postIdentity, Constants.KEY_FUNCTION_POST), new File[] { postFile }));
                    //_browser.createPostURI(to, null, true, readKey);
                } 
                if (_manageForumAuthPost.getPostPBE()) {
                    _ui.debugMessage("pop up a window to post the post identity keys to " + _scope.toBase64() + " with pbe prompt [" + _manageForumAuthPost.getSendPassphrasePrompt() + "] for pass [" + _manageForumAuthPost.getSendPassphrase() + "]");
                    _navControl.view(_uriControl.createPostURI(uri.getScope(), null, _manageForumAuthPost.getSendPassphrase(), _manageForumAuthPost.getSendPassphrasePrompt(), createReferences(uri.getScope(), postIdentity, Constants.KEY_FUNCTION_POST), new File[] { postFile }));
                    //_browser.createPostURI(_scope, null, false, readKey, _viewForumAuthRead.getSendPassphrase(), _viewForumAuthRead.getSendPassphrasePrompt());
                }
            }
            if ( (manageIdentity != null) && (_manageForumAuthManage != null) ) {
                // open a new post to the appropriate locations containing the manage identity's keys
                List<Hash> scopes = _manageForumAuthManage.getSendNewExplicit();
                for (int i = 0; i < scopes.size(); i++) {
                    Hash to = scopes.get(i);
                    _ui.debugMessage("pop up a window to post the manage identity key to " + to.toBase64());
                    _navControl.view(_uriControl.createPostURI(to, null, true, createReferences(uri.getScope(), manageIdentity, Constants.KEY_FUNCTION_MANAGE), new File[] { manageFile }));
                    //_browser.createPostURI(to, null, true, readKey);
                } 
                if (_manageForumAuthManage.getPostPBE()) {
                    _ui.debugMessage("pop up a window to post the manage identity keys to " + _scope + " with pbe prompt [" + _manageForumAuthManage.getSendPassphrasePrompt() + "] for pass [" + _manageForumAuthManage.getSendPassphrase() + "]");
                    _navControl.view(_uriControl.createPostURI(uri.getScope(), null, _manageForumAuthManage.getSendPassphrase(), _manageForumAuthManage.getSendPassphrasePrompt(), createReferences(uri.getScope(), manageIdentity, Constants.KEY_FUNCTION_MANAGE), new File[] { manageFile }));
                    //_browser.createPostURI(_scope, null, false, readKey, _viewForumAuthRead.getSendPassphrase(), _viewForumAuthRead.getSendPassphrasePrompt());
                }
            }
            if ( (_manageForumAuthReply != null) && (_manageForumAuthReply.getRotate()) ) {
                // open a new post to the appropriate locations containing the reply keys
                List<Hash> scopes = _manageForumAuthReply.getSendNewExplicit();
                for (int i = 0; i < scopes.size(); i++) {
                    Hash to = scopes.get(i);
                    _ui.debugMessage("pop up a window to post the reply key to " + to.toBase64());
                    _navControl.view(_uriControl.createPostURI(to, null, true, createReplyReferences(uri.getScope()), null));
                    //_browser.createPostURI(to, null, true, readKey);
                } 
                if (_manageForumAuthReply.getPostPBE()) {
                    _ui.debugMessage("pop up a window to post the reply key to " + _scope.toBase64() + " with pbe prompt [" + _manageForumAuthReply.getSendPassphrasePrompt() + "] for pass [" + _manageForumAuthReply.getSendPassphrase() + "]");
                    _navControl.view(_uriControl.createPostURI(uri.getScope(), null, _manageForumAuthReply.getSendPassphrase(), _manageForumAuthReply.getSendPassphrasePrompt(), createReplyReferences(uri.getScope()), null));
                    //_browser.createPostURI(_scope, null, false, readKey, _viewForumAuthRead.getSendPassphrase(), _viewForumAuthRead.getSendPassphrasePrompt());
                }
            }
            
            // done
            _navControl.unview(_uri);
            if (_scopeId < 0)
                _navControl.view(uri);
            _dataCallback.forumCreated();
        }
    }
    
    private List<ReferenceNode> createReferences(Hash scope, SessionKey readKey) { 
        //_browser.getUI().debugMessage("todo: create references for the read key in " + scope + ": " + readKey);
        SyndieURI uri = SyndieURI.createScope(scope);
        Map attributes = uri.getAttributes();
        attributes.put("readKey", readKey.toBase64());
        ArrayList rv = new ArrayList();
        ReferenceNode node = new ReferenceNode("key", uri, "", "key");
        rv.add(node);
        return rv;
    }

    private List<ReferenceNode> createReferences(Hash scope, Hash postIdentity, String keyFunction) {
        //_browser.getUI().debugMessage("todo: create references for the " + keyFunction + " key in " + scope + ": " + postIdentity);
        SyndieURI uri = SyndieURI.createScope(scope);
        Map attributes = uri.getAttributes();
        List<NymKey> nymKeys = _client.getNymKeys(postIdentity, Constants.KEY_FUNCTION_MANAGE);
        SigningPrivateKey privKey = null;
        for (int i = 0; i < nymKeys.size(); i++) {
            NymKey k = nymKeys.get(i);
            if (k.getData() != null)
                privKey = new SigningPrivateKey(k.getData());
        }
        if (privKey == null) return new ArrayList();
        
        if (Constants.KEY_FUNCTION_POST.equals(keyFunction)) {
            attributes.put("postKey", privKey.toBase64());
        } else {
            attributes.put("manageKey", privKey.toBase64());
        }
        ArrayList rv = new ArrayList();
        ReferenceNode node = new ReferenceNode("key", uri, "", "key");
        rv.add(node);
        return rv;
    }

    private List<ReferenceNode> createReplyReferences(Hash scope) {
        //_browser.getUI().debugMessage("todo: create references for the reply key in " + scope);
        SyndieURI uri = SyndieURI.createScope(scope);
        Map attributes = uri.getAttributes();
        List<NymKey> nymKeys = _client.getNymKeys(scope, Constants.KEY_FUNCTION_REPLY);
        PrivateKey privKey = null;
        for (int i = 0; i < nymKeys.size(); i++) {
            NymKey k = nymKeys.get(i);
            if (k.getData() != null)
                privKey = new PrivateKey(k.getData());
        }
        if (privKey == null) return new ArrayList();
        
        attributes.put("replyKey", privKey.toBase64());
        ArrayList rv = new ArrayList();
        ReferenceNode node = new ReferenceNode("key", uri, "", "key");
        rv.add(node);
        return rv;
    }
    
    public void addListener(StateListener lsnr) { _listeners.add(lsnr); }
    public void removeListener(StateListener lsnr) { _listeners.remove(lsnr); }
    
    void modified() {
        if (!_initialized) return;
        if (!_modified) {
            if (_showActions) {
                _save.setEnabled(true);
                _cancel.setEnabled(true);
            }
            for (int i = 0; i < _listeners.size(); i++)
                _listeners.get(i).settingsModified(true);
        }
        _modified = true;
    }

    private void loadOrigAvatar() {
        byte avatar[] = _client.getChannelAvatar(_scopeId);
        if (avatar != null) {
            Image img = ImageUtil.createImage(avatar);
            if (img != null) {
                Rectangle rect = img.getBounds();
                int width = rect.width;
                int height = rect.height;
                boolean mod = false;
                if (width > Constants.MAX_AVATAR_WIDTH) {
                    width = Constants.MAX_AVATAR_WIDTH;
                    mod = true;
                }
                if (height > Constants.MAX_AVATAR_HEIGHT) {
                    height = Constants.MAX_AVATAR_HEIGHT;
                    mod = true;
                }
                if (mod)
                    img = ImageUtil.resize(img, width, height, true);
                _avatarImgOrig = img;
            } else {
                _avatarImgOrig = null;
            }
        }
    }
    private void populateAvatarMenu() {
        if (_avatarImgOrig != null) { // populated earlier in the initialization
            MenuItem origItem = new MenuItem(_avatarMenu, SWT.PUSH);
            // image not displayed on all platforms
            origItem.setImage(_avatarImgOrig);
            origItem.setText(getText("Current"));
            origItem.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { pickAvatar(_avatarImgOrig); }
                public void widgetSelected(SelectionEvent selectionEvent) { pickAvatar(_avatarImgOrig); }
            });
        }
        int i = 0;
        while (true) {
            final Image img = ImageUtil.createImageFromResource("iconAvatar" + i + ".png");
            if (img != null) {
                _avatarImgStandard.add(img);
                MenuItem item = new MenuItem(_avatarMenu, SWT.PUSH);
                // image not displayed on all platforms - TODO pupup?
                item.setImage(img);
                item.setText(getText("Default") + ' ' + (i+1));
                item.addSelectionListener(new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent selectionEvent) { pickAvatar(img); }
                    public void widgetSelected(SelectionEvent selectionEvent) { pickAvatar(img); }
                });
                i++;
            } else {
                break;
            }
        }
        
        _avatarOther = new MenuItem(_avatarMenu, SWT.PUSH);
        _avatarOther.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { pickAvatar(); }
            public void widgetSelected(SelectionEvent selectionEvent) { pickAvatar(); }
        });
    }
    
    private void pickAvatar(Image img) {
        Image old = _avatarImg;
        if (!_avatarImgStandard.contains(old) && (old != _avatarImgOrig) ) {
            ImageUtil.dispose(old);
        }
        _avatarImg = img;
        _avatar.setImage(img);
        _avatar.setVisible(true);
        _avatar.redraw();
        _avatar.getParent().layout(new Control[] { _avatar });
        if (img != _avatarImgOrig)
            modified();
    }
    private void pickAvatar() {
        FileDialog dialog = new FileDialog(_root.getShell(), SWT.SINGLE | SWT.OPEN);
        dialog.setText(getText("Select a 48x48 pixel PNG image"));
        dialog.setFilterExtensions(new String[] { "*.png" });
        dialog.setFilterNames(new String[] { getText("PNG image") });
        String filename = dialog.open();
        if (filename != null) {
            Image img = ImageUtil.createImageFromFile(filename);
            if (img != null) {
                Rectangle bounds = img.getBounds();
                int width = bounds.width;
                int height = bounds.height;
                if (width > Constants.MAX_AVATAR_WIDTH)
                    width = Constants.MAX_AVATAR_WIDTH;
                if (height > Constants.MAX_AVATAR_HEIGHT)
                    height = Constants.MAX_AVATAR_HEIGHT;
                if ( (height != bounds.height) || (width != bounds.width) ) {
                    img = ImageUtil.resize(img, width, height, true);
                }
                final Image revamped = img;
                int idx = _avatarMenu.indexOf(_avatarOther);
                MenuItem item = new MenuItem(_avatarMenu, SWT.PUSH, idx);
                item.setImage(img);
                item.addSelectionListener(new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent selectionEvent) { pickAvatar(revamped); }
                    public void widgetSelected(SelectionEvent selectionEvent) { pickAvatar(revamped); }
                });
                _avatarImgStandard.add(img);
                pickAvatar(img);
            }
        }
    }
    
    
    private void loadData() { loadData(_client.getChannel(_scopeId)); }
    private void loadData(ChannelInfo info) {
        if (info != null) {
            _description.setText(str(info.getDescription()));
            
            if (info.getExpiration() > 0)
                _expiration.setText(DateTime.getDate(info.getExpiration()));
            
            _name.setText(str(info.getName()));
            
            StringBuilder buf = new StringBuilder();
            for (Iterator iter = info.getPublicTags().iterator(); iter.hasNext(); )
                buf.append((String)iter.next()).append(" ");
            for (Iterator iter = info.getPrivateTags().iterator(); iter.hasNext(); )
                buf.append((String)iter.next()).append(" ");
            _tags.setText(buf.toString());
            
            if (_avatarImgOrig != null)
                pickAvatar(_avatarImgOrig);
        } else {
            if (_avatarImgStandard.size() > 0) {
                Image img = (Image)_avatarImgStandard.get(0);
                _avatarImgOrig = img;
                _avatar.setImage(img);
                _avatar.redraw();
            }
        }
        
        loadArchives(info);
        loadRefs(info);
        loadBans(info);
        loadUsers(info);
        _root.layout(true, true);
        
        _initialized = true;
        _modified = false;
        if (_showActions) {
            _save.setEnabled(false);
            _cancel.setEnabled(false);
        }
        for (int i = 0; i < _listeners.size(); i++)
            _listeners.get(i).settingsModified(false);
    }
    private static final String str(String orig) { return (orig != null ? orig : ""); }
    private void loadArchives(ChannelInfo info) {
        // add buttons w/ menus for the archives in _archiveGroup
        _privArchiveURIs.clear();
        if (info != null) {
            for (Iterator iter = info.getPrivateArchives().iterator(); iter.hasNext(); ) {
                ArchiveInfo archive = (ArchiveInfo)iter.next();
                _privArchiveURIs.add(archive.getURI());
            }
            _pubArchiveURIs.clear();
            for (Iterator iter = info.getPublicArchives().iterator(); iter.hasNext(); ) {
                ArchiveInfo archive = (ArchiveInfo)iter.next();
                _pubArchiveURIs.add(archive.getURI());
            }
        }
        
        redrawArchives();
    }
    private void redrawArchives() {
        int numSelected = 0;
        
        List all = new ArrayList();
        all.addAll(_pubArchiveURIs);
        all.addAll(_privArchiveURIs);
        _ui.debugMessage("redrawArchives: all=" + all.size() + " pub=" + _pubArchiveURIs.size() + " priv=" + _privArchiveURIs.size());
        for (int i = 0; i < all.size(); i++) {
            final SyndieURI uri = (SyndieURI)all.get(i);
            if (uri == null) continue;
            String url = uri.getURL();
            if ( (url == null) || (url.trim().length() <= 0) ) continue;
            numSelected++;
        }
        
        _archiveGroup.setText(getText("Archives") + ": " + numSelected + " ");
        _archiveRemoveAll.setEnabled(numSelected > 0);
        
        _archiveGroup.getParent().layout(new Control[] { _archiveGroup });
    }
    
    private void loadRefs(ChannelInfo info) {
        _referenceNodeRoots.clear();
        if (info != null) {
            List refs = info.getReferences();
            if (refs != null)
                _referenceNodeRoots.addAll(refs);
        }
        redrawRefs();
    }
    private void redrawRefs() {
        int numSelected = 0;
        Counter counter = new Counter(false);
        ReferenceNode.walk(_referenceNodeRoots, counter);
        numSelected = counter.getCount();
        _refGroup.setText(getText("References") + ": " + numSelected + " ");
        _refRemoveAll.setEnabled(numSelected > 0);
        _refGroup.getParent().layout(new Control[] { _refGroup });
    }
    void setReferences(Collection refs) {
        _referenceNodeRoots.clear();
        if (refs != null)
            _referenceNodeRoots.addAll(refs);
        redrawRefs();
        modified();
    }

    void setBanned(List<Hash> scopes) {
        // remove all the old ban refs, and add in new banned refs
        TrimRefs trim = new TrimRefs(true);
        ReferenceNode.walk(_referenceNodeRoots, trim);
        for (int i = 0; i < scopes.size(); i++)
            _referenceNodeRoots.add(new ReferenceNode("banned", SyndieURI.createScope(scopes.get(i)), "", Constants.REF_TYPE_BANNED));
        _banGroup.setText(getText("Bans") + ": " + scopes.size() + " ");
        _banRemoveAll.setEnabled(scopes.size() > 0);
        _banGroup.getParent().layout(new Control[] { _banGroup });
        modified();
    }

    List<ReferenceNode> getRefs() { return _referenceNodeRoots; }

    List<Hash> getBanned() { 
        BannedRefs banned = new BannedRefs();
        ReferenceNode.walk(_referenceNodeRoots, banned);
        return banned.getScopes();
    }
    
    private static class BannedRefs implements ReferenceNode.Visitor {
        private final List<Hash> _scopes;

        public BannedRefs() { _scopes = new ArrayList(); }

        public List<Hash> getScopes() { return _scopes; }

        public void visit(ReferenceNode node, int depth, int siblingOrder) {
            if (node.getURI() == null) return;
            String type = node.getReferenceType();
            if (Constants.REF_TYPE_BANNED.equals(type)) {
                Hash scope = node.getURI().getScope();
                if (scope != null) {
                    if (!_scopes.contains(scope))
                        _scopes.add(scope);
                } else {
                    Hash scopes[] = node.getURI().getSearchScopes();
                    if (scopes != null) {
                        for (int i = 0; i < scopes.length; i++) {
                            if (!_scopes.contains(scopes[i]))
                                _scopes.add(scopes[i]);
                        }
                    }
                }
            }
        }
    }
    
    
    private static class Counter implements ReferenceNode.Visitor {
        private int _count;
        private final boolean _countBanned;

        public Counter(boolean banned) { _countBanned = banned; }

        public int getCount() { return _count; }

        public void visit(ReferenceNode node, int depth, int siblingOrder) { 
            String type = node.getReferenceType();
            if (_countBanned) {
                if ((type != null) && (type.equals(Constants.REF_TYPE_BANNED)))
                    _count++;
            } else if ((type == null) || (!type.equals(Constants.REF_TYPE_BANNED))) {
                _count++;
            }
        }
    }
    
    private void removeRefs() {
        TrimRefs trim = new TrimRefs(false);
        ReferenceNode.walk(_referenceNodeRoots, trim);
        _refGroup.setText(getText("References") + ": " + 0 + " ");
        _refRemoveAll.setEnabled(false);
        _refGroup.getParent().layout(new Control[] { _refGroup });
        modified();
    }
    
    private void removeBans() {
        TrimRefs trim = new TrimRefs(true);
        ReferenceNode.walk(_referenceNodeRoots, trim);
        _banGroup.setText(getText("Bans") + ": " + 0 + " ");
        _banRemoveAll.setEnabled(false);
        _banGroup.getParent().layout(new Control[] { _banGroup });
        modified();
    }
    
    private class TrimRefs implements ReferenceNode.Visitor {
        private final boolean _removeBanned;

        public TrimRefs(boolean removeBanned) { _removeBanned = removeBanned; }

        public void visit(ReferenceNode node, int depth, int siblingOrder) {
            String type = node.getReferenceType();
            if (_removeBanned && !Constants.REF_TYPE_BANNED.equals(type)) {
                // keep it if we only want to remove banned ones and this isn't banned
            } else {
                if (node.getParent() != null)
                    node.getParent().removeChild(node);
                else
                    _referenceNodeRoots.remove(node);
            }
        }
    }
    
    private void loadBans(ChannelInfo info) {
        redrawBans();
    }

    private void redrawBans() {
        Counter counter = new Counter(true);
        ReferenceNode.walk(_referenceNodeRoots, counter);
        int numSelected = counter.getCount();
        _banGroup.setText(getText("Bans") + ": " + numSelected + " ");
        _banRemoveAll.setEnabled(numSelected > 0);
        _banGroup.getParent().layout(new Control[] { _banGroup });
    }

    private void loadUsers(ChannelInfo info) {
        _managerHashes.clear();
        _posterHashes.clear();
        if (info != null) {
            _managerHashes.addAll(info.getAuthorizedManagerHashes());
            _posterHashes.addAll(info.getAuthorizedPosterHashes());
        }
    }
    
    public void applyTheme(Theme theme) {
        _nameLabel.setFont(theme.DEFAULT_FONT);
        _name.setFont(theme.DEFAULT_FONT);
        _tagsLabel.setFont(theme.DEFAULT_FONT);
        _tags.setFont(theme.DEFAULT_FONT);
        _descriptionLabel.setFont(theme.DEFAULT_FONT);
        _description.setFont(theme.DEFAULT_FONT);
        _expirationLabel.setFont(theme.DEFAULT_FONT);
        _expiration.setFont(theme.DEFAULT_FONT);
        _expirationManager.setFont(theme.BUTTON_FONT);
        _archiveGroup.setFont(theme.DEFAULT_FONT);
        _archiveRemoveAll.setFont(theme.DEFAULT_FONT);
        _archiveSelect.setFont(theme.DEFAULT_FONT);
        _refGroup.setFont(theme.DEFAULT_FONT);
        _refRemoveAll.setFont(theme.DEFAULT_FONT);
        _refSelect.setFont(theme.DEFAULT_FONT);
        _banGroup.setFont(theme.DEFAULT_FONT);
        _banRemoveAll.setFont(theme.DEFAULT_FONT);
        _banSelect.setFont(theme.DEFAULT_FONT);
        
        if (_showActions) {
            _save.setFont(theme.BUTTON_FONT);
            _cancel.setFont(theme.BUTTON_FONT);
        }

        _authGroup.setFont(theme.DEFAULT_FONT);
        _authLabel.setFont(theme.DEFAULT_FONT);
        _authRead.setFont(theme.BUTTON_FONT);
        _authPost.setFont(theme.BUTTON_FONT);
        _authManage.setFont(theme.BUTTON_FONT);
        _authReply.setFont(theme.BUTTON_FONT);
        
        redrawArchives();

        _root.pack(true);
    }

    

    
    

    public void translate(TranslationRegistry registry) {
        _nameLabel.setText(registry.getText("Name") + ':');
        _tagsLabel.setText(registry.getText("Tags") + ':');
        _descriptionLabel.setText(registry.getText("Description") + ':');
        _expirationLabel.setText(registry.getText("Expiration") + ':');
        _expirationManager.setText(registry.getText("Manage expiration"));
        if (_showActions) {
            _save.setText(registry.getText("Save changes"));
            _cancel.setText(registry.getText("Cancel changes"));
        }

        _authGroup.setText(registry.getText("Authorization and authentication"));
        _authLabel.setText(registry.getText("Forum authorization and authentication takes four forms - those allowed to read a forum's posts, those allowed to post to a forum, those allowed to manage a forum, and those allowed to read the private replies to forum administrators"));
        _authRead.setText(registry.getText("Read posts"));
        _authPost.setText(registry.getText("Create posts"));
        _authManage.setText(registry.getText("Manage"));
        _authReply.setText(registry.getText("Read forum feedback"));

        _archiveRemoveAll.setText(registry.getText("Remove all"));
        _archiveSelect.setText(registry.getText("Select") + "...");
        
        _refRemoveAll.setText(registry.getText("Remove all"));
        _refSelect.setText(registry.getText("Select") + "...");
        
        _banRemoveAll.setText(registry.getText("Remove all"));
        _banSelect.setText(registry.getText("Select") + "...");
    
        _avatar.setToolTipText(registry.getText("Click to change avatar"));
        _avatarOther.setText(registry.getText("Load from file"));
    }
}
