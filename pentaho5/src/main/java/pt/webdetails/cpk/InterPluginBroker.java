/*!
* Copyright 2002 - 2018 Webdetails, a Hitachi Vantara company.  All rights reserved.
*
* This software was developed by Webdetails and is provided under the terms
* of the Mozilla Public License, Version 2.0, or any later version. You may not use
* this file except in compliance with the license. If you need a copy of the license,
* please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
*
* Software distributed under the Mozilla Public License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to
* the license for the specific language governing your rights and limitations.
*/
package pt.webdetails.cpk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import pt.webdetails.cpf.PluginEnvironment;
import pt.webdetails.cpf.PluginSettings;
import pt.webdetails.cpf.plugin.CorePlugin;
import pt.webdetails.cpf.plugincall.api.IPluginCall;
import pt.webdetails.cpf.plugincall.base.CallParameters;
import pt.webdetails.cpf.utils.PluginIOUtils;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class InterPluginBroker {
  private static Log logger = LogFactory.getLog( InterPluginBroker.class );

  static final String CDE_RENDER_API_BEAN_ID_TAG = "cde-render-api-bean-id";

  static final String CDE_RENDER_API_BEAN_ID = "renderApi";
  static final String CDE_RENDER_API_LEGACY_BEAN_ID = "renderer";

  static final String CDE_RENDER_API_RENDER_METHOD_TAG = "cde-render-api-render-method";
  static final String CDE_RENDER_API_RENDER_METHOD = "render";

  private static IPluginCall cdeRenderApiCall;

  public static void run( Map<String, Object> params, OutputStream out ) throws Exception {
    IPluginCall cdeRenderer = getCdeRenderer();
    if ( cdeRenderer == null ) {
      logger.error( "No InterPluginCall found for CDE Renderer." );

      return;
    }

    CallParameters parameters = new CallParameters();
    for ( String key : params.keySet() ) {
      String value = params.get( key ).toString();

      parameters.put( key, value );
    }

    String response = cdeRenderer.call( parameters.getParameters() );
    PluginIOUtils.writeOutAndFlush( out, response );
  }

  private static IPluginCall getCdeRenderer() {
    if ( cdeRenderApiCall != null ) {
      return cdeRenderApiCall;
    }

    final String pluginID = CorePlugin.CDE.getId();
    final PluginEnvironment environment = PluginEnvironment.env();

    // 1. try configured values
    String beanId = getSettingValue( CDE_RENDER_API_BEAN_ID_TAG, "" );
    String method = getSettingValue( CDE_RENDER_API_RENDER_METHOD_TAG, "" );

    IPluginCall cdePluginCall = environment.getPluginCall( pluginID, beanId, method );
    if ( cdePluginCall.exists() ) {
      return ( cdeRenderApiCall = cdePluginCall );
    }

    // 2. fallback to latest cde render bean id
    cdePluginCall = environment.getPluginCall( pluginID, CDE_RENDER_API_BEAN_ID, CDE_RENDER_API_RENDER_METHOD );
    if ( cdePluginCall.exists() ) {
      return ( cdeRenderApiCall = cdePluginCall );
    }

    // 3. fallback to legacy cde render bean id
    cdePluginCall = environment.getPluginCall( pluginID, CDE_RENDER_API_LEGACY_BEAN_ID, CDE_RENDER_API_RENDER_METHOD );
    if ( cdePluginCall.exists() ) {
      return ( cdeRenderApiCall = cdePluginCall );
    }

    return null;
  }

  private static String getSettingValue( String tag, String defaultValue ) {
    final PluginSettings settings = PluginEnvironment.env().getPluginSettings();

    List<String> values = settings.getTagValue( tag );
    return values.isEmpty() ? defaultValue : values.get( 0 );
  }

}
