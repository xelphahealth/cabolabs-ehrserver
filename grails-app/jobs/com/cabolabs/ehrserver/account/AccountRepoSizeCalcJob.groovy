package com.cabolabs.ehrserver.account

import com.cabolabs.ehrserver.versions.VersionFSRepoService

class AccountRepoSizeCalcJob {

   def concurrent = false

   def optService
   def versionRepoService

   static triggers = {
      simple repeatInterval: 600000l, startDelay: 240000l // execute job once in x/1000 seconds
   }

   def execute()
   {
      log.info('updating disk usage per account')
      Account.list().each { account ->

         account.current_opt_repo_size = optService.getRepoSizeInBytesAccount(account)
         account.current_version_repo_size = versionRepoService.getRepoSizeInBytesAccount(account)
         account.save(failOnError: true)
      }
   }
}
