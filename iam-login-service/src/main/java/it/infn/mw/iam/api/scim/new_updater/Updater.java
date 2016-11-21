package it.infn.mw.iam.api.scim.new_updater;

/**
 * And updater attempts to update something, and returns true if that something was actually updated
 *
 */
public interface Updater {

  /**
   * The updater update logic
   * 
   * @return <ul><li><code>true</code>, if the object was modified by the update</li>
   * <li><code>false</code>, otherwise</li> </ul>
   */
  boolean update();
}