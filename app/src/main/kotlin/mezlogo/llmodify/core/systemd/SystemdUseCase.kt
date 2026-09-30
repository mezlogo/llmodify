package mezlogo.llmodify.core.systemd

interface SystemdUseCase {

  /** `systemctl list-unit-files --type=service --state=enabled --no-legend | cut -d' ' -f1` */
  fun getEnabledSystemServices(): Set<String>

  /**
   * `systemctl list-unit-files --user --type=service --state=enabled --no-legend | cut -d' ' -f1`
   */
  fun getEnabledUserServices(): Set<String>

  /** `sudo systemctl enable $SERVICE` */
  fun enableServices(service: String): Boolean

  /** `systemctl --user enable $SERVICE` */
  fun enableUserServices(service: String): Boolean
}
