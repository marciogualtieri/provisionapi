package models

import java.sql.Timestamp

case class Instance(id: Long,
                    name: String,
                    plan: String,
                    state: String,
                    targetId: String,
                    created: Timestamp,
                    updated: Timestamp) {

  def withState(state: String): Instance = {
    new Instance(this.id, this.name, this.plan, state, this.targetId, created, this.updated)
  }

  def withUpdated(updated: Timestamp): Instance = {
    new Instance(this.id, this.name, this.plan, this.state, this.targetId, this.created, updated)
  }
}