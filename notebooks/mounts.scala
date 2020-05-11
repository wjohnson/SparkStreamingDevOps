val storageAccountName = ""
val containerName = "events"
val directoryName = "eventOutput"
val confKey = s"fs.azure.account.key.$storageAccountName.blob.core.windows.net"

dbutils.fs.mount(
  source = s"wasbs://$containerName@$storageAccountName.blob.core.windows.net/$directoryName",
  mountPoint = "/mnt/eventOutput",
  extraConfigs = Map(confKey -> dbutils.secrets.get(scope = "myproject", key = "storagekey"))
)