package com.rmsi.android.mast.Fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.rmsi.android.mast.activity.AddPersonActivity;
import com.rmsi.android.mast.activity.CaptureTenureInfo;
import com.rmsi.android.mast.activity.ListActivity;
import com.rmsi.android.mast.activity.Owner;
import com.rmsi.android.mast.activity.R;
import com.rmsi.android.mast.adapter.PersonListAdapter;
import com.rmsi.android.mast.adapter.ResourcePersonListAdapter;
import com.rmsi.android.mast.db.DbController;
import com.rmsi.android.mast.domain.Attribute;
import com.rmsi.android.mast.domain.Media;
import com.rmsi.android.mast.domain.Person;
import com.rmsi.android.mast.domain.Property;
import com.rmsi.android.mast.domain.ResourceCustomAttribute;
import com.rmsi.android.mast.domain.ResourceOwner;
import com.rmsi.android.mast.util.CommonFunctions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.rmsi.android.mast.util.CommonFunctions.getApplicationContext;

/**
 * Created by Ambar.Srivastava on 1/15/2018.
 */

public class ResourcePersonListFragment extends ListFragment implements ListActivity {
    private Context context;
    private ResourcePersonListAdapter adapter;
    private List<Attribute> persons;
    private List<ResourceOwner> listOwner;

    private String mediaFolderName = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
            CommonFunctions.parentFolderName + File.separator + CommonFunctions.mediaFolderName;
    private File file;
    private String timeStamp = new SimpleDateFormat("MMdd_HHmmss").format(new Date().getTime());
    private int position = 0;
    private boolean readOnly = false;
    private String tenureType;

    public ResourcePersonListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {


            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    1024);
        }



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        return view;
    }

    /**
     * Sets persons list
     */
    public void setPersons(List<ResourceOwner> listOwner, boolean readOnly, String tenureType, long featureID, int iOwnerCount) {
        this.listOwner = listOwner;
        this.readOnly = readOnly;
        this.tenureType=tenureType;
        adapter = new ResourcePersonListAdapter(context, this, listOwner,tenureType,featureID,iOwnerCount);
        setListAdapter(adapter);
        refresh();
    }

    @Override
    public void showPopup(View v, int position) {
        PopupMenu popup = new PopupMenu(context, v);
        MenuInflater inflater = popup.getMenuInflater();
        if (!readOnly) {
            inflater.inflate(R.menu.attribute_listing_options_for_person, popup.getMenu());
        } else {
            inflater.inflate(R.menu.attribute_listing_options_to_view_details, popup.getMenu());
        }

        this.position = position;
        final ResourceOwner resourceOwner = listOwner.get(position);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.edit_attributes:
                        Intent myIntent = new Intent(context, Owner.class);
                        myIntent.putExtra("groupid", resourceOwner.getGroupId());
                        myIntent.putExtra("featureid", resourceOwner.getFeatureID());
                        try {


                            Property property= DbController.getInstance(context).getClassi(resourceOwner.getFeatureID());
                            myIntent.putExtra("tenure", property.getTenureTypeValue());
                            // String selectQueryOptions = "SELECT * FROM" + " " + type.getTableName() + " WHERE FLAG " + "=" + "'Resource'" +" ORDER BY LISTING";
                            String tenureID=DbController.getInstance(context).getTenureID(property.getTenureTypeValue());
                            myIntent.putExtra("tID", tenureID);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        startActivity(myIntent);
                        return true;
                    case R.id.add_image:
                        if (resourceOwner.getMedia() != null && resourceOwner.getMedia().size() > 0) {
                            CommonFunctions.getInstance()
                                    .showMessage(context,
                                            getResources().getString(R.string.info),
                                            getResources().getString(R.string.you_can_add_only_one_photo)
                                    );
                        }
                        else {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                timeStamp = new SimpleDateFormat("MMdd_HHmmss").format(new Date().getTime());
                                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                                file = new File(mediaFolderName + File.separator + "mast_" + timeStamp + ".jpg");
                                if (file != null) {

                                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
//                                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,  FileProvider.getUriForFile(getActivity(),
//                                            BuildConfig.APPLICATION_ID + ".provider",
//                                            file));
                                    cameraIntent.putExtra("ID", resourceOwner.getGroupId());
//                                startActivityForResult(cameraIntent, 1);
                                    cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    if (cameraIntent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
                                        startActivityForResult(cameraIntent, 1);
                                    }
                                } else {
                                    Toast.makeText(context, getResources().getString(R.string.unable_to_capture), Toast.LENGTH_LONG).show();
                                }
                            }
                            else{

                                timeStamp = new SimpleDateFormat("MMdd_HHmmss").format(new Date().getTime());
                                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                                file = new File(mediaFolderName + File.separator + "mast_" + timeStamp + ".jpg");
                                if (file != null) {
                                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
//                                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,  FileProvider.getUriForFile(getActivity(),
//                                            BuildConfig.APPLICATION_ID + ".provider",
//                                            file));
                                    cameraIntent.putExtra("ID", resourceOwner.getGroupId());
                                    startActivityForResult(cameraIntent, 1);
                                    //cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                                    if (cameraIntent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
//                                        startActivityForResult(cameraIntent, 1);
//                                    }
                                } else {
                                    Toast.makeText(context, getResources().getString(R.string.unable_to_capture), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                        return true;

                    case R.id.delete_photo:
                        deletePhoto(resourceOwner);
                        return true;

                    case R.id.delete_entry:
                        delete(resourceOwner);
                        return true;

                    case R.id.view_attributes:
                        Intent intent = new Intent(context, Owner.class);
                        intent.putExtra("groupid", resourceOwner.getGroupId());
                        intent.putExtra("featureid", resourceOwner.getFeatureID());
//                        intent.putExtra("personSubType", person.getSubTypeId());
//                        intent.putExtra("rightId", person.getRightId());
                        intent.putExtra("readOnly", true);
                        startActivity(intent);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }
    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1024) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Now user should be able to use camera
            }
            else {
                // Your app will not have this permission. Turn off all functions
                // that require this permission or it will force close like your
                // original question
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        ProgressDialog ringProgressDialog = null;
        CommonFunctions.getInstance().loadLocale(context);
        if (requestCode == 1) //Image
        {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    //photo = rotate(photo, 90);
                    Media media = new Media();
                    if (file == null) {
                        String filename = mediaFolderName + File.separator + "mast_" + timeStamp + ".jpg";
                        System.out.println("filename=" + filename);
                        file = new File(filename);
                        if (file != null && file.exists())
                            CommonFunctions.getInstance().addErrorMessage("PersonListActivity", "Problem Adding file with name:" + filename);
                    }
                    if (file != null && file.exists()) {
                        compressImage();
                        media.setPath(file.getAbsolutePath());
                        media.setFeatureId(listOwner.get(position).getFeatureID());
                        media.setType(Media.TYPE_PHOTO);
                        media.setPersonId((long) listOwner.get(position).getGroupId());
                        media.setFlag("R");
                        boolean result = DbController.getInstance(context).saveMedia(media);

                        if (result){
                            Toast.makeText(context, R.string.pic_added_successfully, Toast.LENGTH_LONG).show();
                            listOwner.get(position).getMedia().add(media);
                            refresh();
                        }
                        else
                            Toast.makeText(context, R.string.unable_to_capture, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context, R.string.unable_to_capture, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e1) {
                    CommonFunctions.getInstance().appLog("", e1);
                    e1.printStackTrace();
                } finally {
                    if (ringProgressDialog != null)
                        ringProgressDialog.dismiss();
                }
            }
        }
    }

    private void compressImage() {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            //options.inJustDecodeBounds = true;
            //BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            //options.inSampleSize =2;   //calculateInSampleSize(options,768,1024);

            options.inJustDecodeBounds = false;
            Bitmap resizedPhoto = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            int height = 600;
            int width = 600;
            double ratio = (double)height/(double) width;

            int imgHeight = resizedPhoto.getHeight();
            int imgWidth = resizedPhoto.getWidth();

            // Cut the image if width is greater than height
            if(imgHeight < imgWidth) {
                int imgWidthNew = (int) (imgHeight / ratio);
                resizedPhoto = Bitmap.createBitmap(resizedPhoto, (imgWidth-imgWidthNew)/2, 0, imgWidthNew, imgHeight);
            } else {
                // Cut the image if height is greater than width
                int imgHeightNew = (int) (imgWidth * ratio);
                if(imgHeightNew < imgHeight)
                    resizedPhoto = Bitmap.createBitmap(resizedPhoto, 0, (imgHeight-imgHeight)/2, imgWidth, imgHeightNew);
            }

            ByteArrayOutputStream outFile = new ByteArrayOutputStream();

            // Rescale only if photo is bigger than required size
            if(resizedPhoto.getWidth() > width) {
                resizedPhoto = Bitmap.createScaledBitmap(resizedPhoto, width, height, true);
            }

            resizedPhoto.compress(Bitmap.CompressFormat.JPEG, 60, outFile);
            outFile.size();

            if ((outFile.size() / 1024) > 150) {
                Toast.makeText(context, "File Length-->" + (outFile.size() / 1024), Toast.LENGTH_LONG).show();
                resizedPhoto = BitmapFactory.decodeByteArray(outFile.toByteArray(), 0, outFile.toByteArray().length);
                outFile = new ByteArrayOutputStream();
                resizedPhoto.compress(Bitmap.CompressFormat.JPEG, 40, outFile);
            }

            FileOutputStream fo = new FileOutputStream(file.getAbsolutePath());
            fo.write(outFile.toByteArray());
            fo.flush();
            fo.close();
        } catch (Exception e) {
            Toast.makeText(context, "unable to compress image", Toast.LENGTH_SHORT).show();
            CommonFunctions.getInstance().appLog("", e);
            e.printStackTrace();
        }
    }

    private void delete(final ResourceOwner person) {
        Property prop = DbController.getInstance(context).getProperty(person.getFeatureID());

        if (prop != null) {

                if (prop.getResPersonOfInterests() != null && prop.getResPersonOfInterests().size() > 0) {
                    CommonFunctions.getInstance().showMessage(
                            context,
                            getResources().getString(R.string.warning),
                            getResources().getString(R.string.delete_poi_first)
                    );

            } else {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                alertDialogBuilder.setMessage(R.string.deleteEntryMsg);
                alertDialogBuilder.setPositiveButton(R.string.btn_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                if (DbController.getInstance(context).deleteOwner((long) person.getGroupId())) {
                                    listOwner.remove(person);
                                    refresh();
                                } else {
                                    Toast.makeText(context, "Unable to delete", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                alertDialogBuilder.setNegativeButton(R.string.btn_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }
    }

    /**
     * Refreshes the list
     */
    public void refresh() {
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    private void deletePhoto(final ResourceOwner person) {
        if (person.getMedia() != null && person.getMedia().size() > 0) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            alertDialogBuilder.setMessage(R.string.alert_delete_photo);
            alertDialogBuilder.setPositiveButton(R.string.btn_ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            boolean isDeleted = false;
                            Iterator<Media> iterator = person.getMedia().iterator();
                            while (iterator.hasNext()){
                                Media media = iterator.next();
                                if (DbController.getInstance(context).deleteMedia(media.getId())) {
                                    isDeleted = true;
                                    iterator.remove();
                                }
                            }
                            if (isDeleted) {
                                refresh();
                                Toast.makeText(context, R.string.pic_delete_msg, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(context, "error", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

            alertDialogBuilder.setNegativeButton(R.string.btn_cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        } else {
            Toast.makeText(context, R.string.no_pic_person, Toast.LENGTH_LONG).show();
        }
    }


//    private void deletePhoto(final Person person) {
//        if (person.getMedia() != null && person.getMedia().size() > 0) {
//            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
//            alertDialogBuilder.setMessage(R.string.alert_delete_photo);
//            alertDialogBuilder.setPositiveButton(R.string.btn_ok,
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface arg0, int arg1) {
//                            boolean isDeleted = false;
//                            Iterator<Media> iterator = person.getMedia().iterator();
//                            while (iterator.hasNext()){
//                                Media media = iterator.next();
//                                if (DbController.getInstance(context).deleteMedia(media.getId())) {
//                                    isDeleted = true;
//                                    iterator.remove();
//                                }
//                            }
//                            if (isDeleted) {
//                                refresh();
//                                Toast.makeText(context, R.string.pic_delete_msg, Toast.LENGTH_LONG).show();
//                            } else {
//                                Toast.makeText(context, "error", Toast.LENGTH_LONG).show();
//                            }
//                        }
//                    });
//
//            alertDialogBuilder.setNegativeButton(R.string.btn_cancel,
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
//                        }
//                    });
//
//            AlertDialog alertDialog = alertDialogBuilder.create();
//            alertDialog.show();
//        } else {
//            Toast.makeText(context, R.string.no_pic_person, Toast.LENGTH_LONG).show();
//        }
//    }
    public boolean hasPermissionInManifest(Context context, String permissionName) {
        final String packageName = context.getPackageName();
        try {
            final PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            final String[] declaredPermisisons = packageInfo.requestedPermissions;
            if (declaredPermisisons != null && declaredPermisisons.length > 0) {
                for (String p : declaredPermisisons) {
                    if (p.equals(permissionName)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {

        }
        return false;
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
