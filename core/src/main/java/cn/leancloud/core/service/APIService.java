package cn.leancloud.core.service;

import cn.leancloud.core.AVObject;
import cn.leancloud.core.AVRole;
import cn.leancloud.core.AVUser;
import cn.leancloud.core.types.AVDate;
import cn.leancloud.upload.FileUploadToken;
import io.reactivex.Observable;
import retrofit2.http.*;

import java.util.List;

import com.alibaba.fastjson.*;

public interface APIService {
  /**
   * Object Operations.
   */

  @GET("/1.1/classes/{className}")
  Observable<List<AVObject>> findObjects(@Path("className") String className);

  @GET("/1.1/classes/{className}/{objectId}")
  Observable<AVObject> fetchObject(@Path("className") String className, @Path("objectId") String objectId);

  @POST("/1.1/classes/{className}")
  Observable<AVObject> createObject(@Path("className") String className, @Body JSONObject object);

  @PUT("/1.1/classes/{className}/{objectId}")
  Observable<AVObject> updateObject(@Path("className") String className, @Path("objectId") String objectId,
                                    @Body JSONObject object);

  @DELETE("/1.1/classes/{className}/{objectId}")
  Observable<Void> deleteObject(@Path("className") String className, @Path("objectId") String objectId);

  @POST("/1.1/filetokens")
  Observable<FileUploadToken> createUploadToken(@Body String fileData);

  @POST("/1.1/fileCallback")
  Observable<Void> fileCallback(@Body String result);

  @GET("/1.1/date")
  Observable<AVDate> currentTimeMillis();

  @POST("/1.1/batch")
  Observable<Void> batchSave(@Body JSONObject object);

  /**
   * Role Operations.
   */
  @POST("/1.1/roles")
  Observable<AVRole> createRole(@Body JSONObject object);

  /**
   * User Operations.
   */

  @POST("/1.1/users")
  Observable<AVUser> signup(@Body JSONObject object);

  @POST("/1.1/usersByMobilePhone")
  Observable<AVUser> signupByMobilePhone(@Body JSONObject object);

  @POST("/1.1/login")
  Observable<AVUser> login(@Body JSONObject object);

  @PUT("/1.1/users/{objectId}/updatePassword")
  Observable<AVUser> updatePassword(@Path("objectId") String objectId, @Body JSONObject object);
}
