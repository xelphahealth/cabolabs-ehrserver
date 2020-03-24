/*
 * Copyright 2011-2020 CaboLabs Health Informatics
 *
 * The EHRServer was designed and developed by Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> at CaboLabs Health Informatics (www.cabolabs.com).
 *
 * You can't remove this notice from the source code, you can't remove the "Powered by CaboLabs" from the UI, you can't remove this notice from the window that appears then the "Powered by CaboLabs" link is clicked.
 *
 * Any modifications to the provided source code can be stated below this notice.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ehr

import com.cabolabs.ehrserver.openehr.directory.Folder
import com.cabolabs.ehrserver.openehr.ehr.Ehr
import com.cabolabs.security.Organization

class EhrTagLib {

   def authService

   def hasEhr = { attrs, body ->

      if (!attrs.patientUID) throw new Exception("patientUID es obligatorio")

      //println patientUID

      def c = Ehr.createCriteria()

      def ehr = c.get {
         subject {
            eq ('value', attrs.patientUID)
         }
      }

      //println ehr

      if (ehr) out << body()
   }

   def dontHasEhr = { attrs, body ->

      if (!attrs.patientUID) throw new Exception("patientUID es obligatorio")

      //println attrs.patientUID

      def c = Ehr.createCriteria()

      def ehr = c.get {
         subject {
            eq ('value', attrs.patientUID)
         }
      }

      //println ehr

      if (!ehr) out << body()
   }

   def ehr_directory = { attrs, body ->

      if (!attrs.directory) return

      out << recursive_directory(attrs.directory)
   }

   private String recursive_directory(Folder folder)
   {
      def html = $/
      |<div class="folder">
      |  <div class="folder_name">
      |    <input type="radio" name="folder.id" value="${folder.id}" />
      |    ${folder.name} (${folder.items.size()})
      |  </div>
      |  <div class="folder_items">
      /$.stripMargin()

      folder.items.each {
         // Versioned Composition: UID
         html += '<div class="folder_item">Versioned Composition: '+ g.link(controller:'versionedComposition', action:'show', params:[uid:it], it) +'</div>'
      }

      html += '</div><div class="folder_folders">'

      folder.folders.each {
         html += recursive_directory(it)
      }

      html += '</div></div>' // /folder_folders, /folder

      return html
   }

   def canEditUser = { attrs, body ->

      if (authService.loggedInUserHasAnyRole("ROLE_ADMIN"))
      {
         out << body()
      }
      else
      {
         def userInstance = attrs.userInstance // user to edit
         def userHigherRole = userInstance.getHigherAuthority(session.organization)

         def loggedInUser = authService.loggedInUser
         if(loggedInUser)
         {
            def role = loggedInUser.getHigherAuthority(session.organization)

            // if the logged user has a role higher than the highest role of the user, he can edit it.
            if (role.higherThan(userHigherRole))
            {
               out << body()
            }
         }
      }
   }

   def selectWithCurrentUserOrganizations = { attrs, body ->

      def loggedInUser = authService.loggedInUser
      if(loggedInUser)
      {
         def args = [:]

         // admins will see every org
         if (authService.userHasAnyRole(loggedInUser, "ROLE_ADMIN"))
         {
            args.from = Organization.list()
         }
         else
         {
            args.from = loggedInUser.organizations
         }

         args.optionKey = 'uid'
         args.optionValue = 'name' //{it.name +' '+ it.uid} //'name'

         if (attrs.addEmpty)
            args.noSelection = ['':message(code:'defaut.select.selectOne')] // TODO: i18n

         args.class = attrs.class ?: '' // allows set style from outside
         args.value = session.organization.uid

         if (attrs.multiple)
         {
            args.multiple = 'true'
            args.size = 5
         }

         // add the rest of the attrs to the select args, name, value, class, etc
         args += attrs

         out << g.select(args) // name:attrs.name, from:orgs, optionKey:'uid', optionValue:'name', value:attrs.value
      }
   }

}
