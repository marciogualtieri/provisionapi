import java.sql.Timestamp

import javax.inject.Singleton

package object utils {
  @Singleton
  class TimeUtils {
    def now(): Timestamp = new Timestamp(System.currentTimeMillis())
  }
}
