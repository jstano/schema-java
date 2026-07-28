do $createextensions$
begin
   if (select usesuper from pg_user where usename = CURRENT_USER) then
      create extension if not exists "citext";
      create extension if not exists "btree_gist";
   else
      raise notice 'User % is not a superuser, could not create extensions.', current_user;
   end if;
end;
$createextensions$;

drop type if exists domain_type cascade;
create type domain_type as enum ('LDAP','SAML','UNIPORTAL');

drop type if exists required_user_type cascade;
create type required_user_type as enum ('LOYALTY','RMS','OTHER');

drop type if exists registration_type cascade;
create type registration_type as enum ('EMPLOYEE','USER');

drop type if exists audit_change_type cascade;
create type audit_change_type as enum ('A','D','M');

drop type if exists property_type cascade;
create type property_type as enum ('FULL_SERVICE_HOTEL','SELECT_SERVICE_HOTEL','EXTENDED_STAY_HOTEL','RESORT','BOUTIQUE_HOTEL','CONVENTION_HOTEL','CASINO_HOTEL','MIXED_USE','LIMITED_SERVICE_HOTEL','ALL_INCLUSIVE_RESORT','TIMESHARE','GOLF_COURSE','RESTAURANT','OTHER');

drop type if exists market_type cascade;
create type market_type as enum ('URBAN','SUBURBAN','AIRPORT','RESORT','SMALL_TOWN','HIGHWAY','CONVENTION','CAMPUS','OTHER');

/* public.AccessURL */
drop table if exists public.AccessURL cascade;

create table public.AccessURL
(
   AccessURL text not null,
   PortalClientID integer not null,
   constraint pk_accessurl primary key (AccessURL)
);

/* public.ApiAuthorization */
drop table if exists public.ApiAuthorization cascade;

create table public.ApiAuthorization
(
   ID serial not null,
   PortalUserID integer,
   ApiSecret text,
   RefreshToken text,
   constraint pk_apiauthorization primary key (ApiSecret)
);

create index ix_apiauthorization1 on public.ApiAuthorization (PortalUserID);

/* public.Audit */
drop table if exists public.Audit cascade;

create table public.Audit
(
   ID bigserial not null,
   ChangedBy text not null,
   ChangedOn timestamp not null,
   RecordID text,
   RecordName text not null,
   Category text not null,
   Item text not null,
   Change audit_change_type not null,
   OldValue text,
   NewValue text,
   PortalClientID integer not null,
   constraint pk_audit primary key (ID)
);

create index ix_audit1 on public.Audit (ChangedOn,PortalClientID);

/* public.AutomationAuthorization */
drop table if exists public.AutomationAuthorization cascade;

create table public.AutomationAuthorization
(
   AutomationSecret uuid not null default uuidv7(),
   RefreshToken text not null,
   ExpirationDateTime timestamp not null,
   CreatedOnDateTime timestamp not null,
   LastUsedDateTime timestamp,
   PortalUserID integer not null,
   constraint pk_automationauthorization primary key (AutomationSecret)
);

create index ix_automationauthorization1 on public.AutomationAuthorization (PortalUserID);

/* public.Brand */
drop table if exists public.Brand cascade;

create table public.Brand
(
   ID serial not null,
   Name text not null,
   constraint pk_brand primary key (ID),
   constraint ak_brand1 unique (Name)
);

insert into Brand (Name) values ('Hilton');
            insert into Brand (Name) values ('Marriott International');
            insert into Brand (Name) values ('Hyatt Hotels Corporation');
            insert into Brand (Name) values ('IHG Hotels & Resorts');
            insert into Brand (Name) values ('Choice Hotels');
            insert into Brand (Name) values ('Wyndham Hotels & Resorts');
            insert into Brand (Name) values ('Other Notable Brands & Independent Collections');;

/* public.Certificate */
drop table if exists public.Certificate cascade;

create table public.Certificate
(
   Alias text not null,
   Certificate text,
   PrivateKey text,
   constraint pk_certificate primary key (Alias)
);

/* public.ClientPreference */
drop table if exists public.ClientPreference cascade;

create table public.ClientPreference
(
   ID bigserial not null,
   PortalClientID integer not null,
   PrefKey text not null,
   PrefValue text not null,
   constraint pk_clientpreference primary key (ID),
   constraint ak_clientpreference1 unique (PortalClientID,PrefKey)
);

/* public.ClientProduct */
drop table if exists public.ClientProduct cascade;

create table public.ClientProduct
(
   ClientID integer not null,
   ProductID integer not null,
   constraint pk_clientproduct primary key (ClientID,ProductID)
);

/* public.ClientRegion */
drop table if exists public.ClientRegion cascade;

create table public.ClientRegion
(
   ID serial not null,
   PortalClientID integer not null,
   Name text not null,
   ShortName text not null,
   Code text not null,
   ExcludeFromCorpReports char(1) not null default 'N',
   constraint pk_clientregion primary key (ID),
   constraint ak_clientregion1 unique (PortalClientID,Name),
   constraint ak_clientregion2 unique (PortalClientID,Code),
   constraint ck_clientreg_excludefr_857B8591 check(ExcludeFromCorpReports in ('Y','N'))
);

/* public.CredentialsHistory */
drop table if exists public.CredentialsHistory cascade;

create table public.CredentialsHistory
(
   Credentials text not null,
   CredentialsDateTime timestamp not null,
   constraint pk_credentialshistory primary key (Credentials)
);

create index ix_credentialshistory1 on public.CredentialsHistory (CredentialsDateTime);

/* public.ExpiredCredentials */
drop table if exists public.ExpiredCredentials cascade;

create table public.ExpiredCredentials
(
   Credentials text not null,
   ExpiredOnDateTime timestamp not null,
   constraint pk_expiredcredentials primary key (Credentials)
);

/* public.GlobalData */
drop table if exists public.GlobalData cascade;

create table public.GlobalData
(
   ID serial not null,
   Tag text not null,
   TagValue text,
   TagData bytea,
   constraint pk_globaldata primary key (ID),
   constraint ak_globaldata1 unique (Tag)
);

/* public.ImpersonateHistory */
drop table if exists public.ImpersonateHistory cascade;

create table public.ImpersonateHistory
(
   PortalUserIDToImpersonate integer not null,
   PortalUserIDOfImpersonator integer not null,
   LoginDateTime timestamp not null,
   constraint pk_impersonatehistory primary key (PortalUserIDToImpersonate,PortalUserIDOfImpersonator,LoginDateTime)
);

/* public.LDAPConfig */
drop table if exists public.LDAPConfig cascade;

create table public.LDAPConfig
(
   ID serial not null,
   URL text not null,
   BaseDN text not null,
   SearchFilter text not null,
   Domain text,
   UserNamePattern text,
   constraint pk_ldapconfig primary key (ID)
);

/* public.LoginHistory */
drop table if exists public.LoginHistory cascade;

create table public.LoginHistory
(
   ID serial not null,
   PortalUserID integer,
   LoginDateTime timestamp not null,
   LoginStatus text not null,
   DatabaseID integer,
   PropertyID integer,
   PropertyName text,
   LoginID text,
   FailureReason text,
   constraint pk_loginhistory primary key (PortalUserID,LoginDateTime,LoginStatus)
);

/* public.NewUserRegistration */
drop table if exists public.NewUserRegistration cascade;

create table public.NewUserRegistration
(
   ID uuid not null default uuidv7(),
   Type registration_type not null,
   PartnerCode text not null,
   LoginID text not null,
   VerificationCode text not null,
   ClientID integer not null,
   LoginClientID integer not null,
   CreatedOn timestamp not null,
   CreatedByPortalUserID integer not null,
   UpdatedByPortalUserID integer,
   ExpiresOn timestamp not null,
   NumberVerifyFailures integer not null,
   NumberOfAttempts integer,
   StartedOn timestamp,
   CompletedOn timestamp,
   PortalUserID integer,
   DatabaseID integer,
   PropertyID integer,
   EmployeeID integer,
   Email text,
   MobilePhone text,
   FirstName text,
   MiddleName text,
   LastName text,
   Locale text,
   constraint pk_newuserregistration primary key (ID),
   constraint ak_newuserregistration1 unique (PartnerCode,LoginID,VerificationCode)
);

/* public.PasswordBlacklist */
drop table if exists public.PasswordBlacklist cascade;

create table public.PasswordBlacklist
(
   ID serial not null,
   PasswordPattern text not null,
   constraint pk_passwordblacklist primary key (ID),
   constraint ak_passwordblacklist1 unique (PasswordPattern)
);

/* public.PasswordQuestion */
drop table if exists public.PasswordQuestion cascade;

create table public.PasswordQuestion
(
   ID serial not null,
   Locale text not null,
   Question text not null,
   constraint pk_passwordquestion primary key (ID),
   constraint ak_passwordquestion1 unique (Locale,Question)
);

/* public.PasswordRules */
drop table if exists public.PasswordRules cascade;

create table public.PasswordRules
(
   ID serial not null,
   Pattern text not null,
   Description text not null,
   Duration smallint,
   ExpirationWarning smallint,
   NumberPrior smallint,
   MaxAttempts smallint,
   ResetsPerDay integer not null default 3,
   constraint pk_passwordrules primary key (ID),
   constraint ck_passwordr_duration_D74EEB37 check(Duration >= 1),
   constraint ck_passwordr_expiratio_EF8FDBCA check(ExpirationWarning >= 1),
   constraint ck_passwordr_numberpri_F247D89E check(NumberPrior >= 0),
   constraint ck_passwordr_resetsper_E8192F86 check(ResetsPerDay >= 1 and ResetsPerDay <= 3)
);

/* public.PortalAdminUserAdditionalClients */
drop table if exists public.PortalAdminUserAdditionalClients cascade;

create table public.PortalAdminUserAdditionalClients
(
   ID serial not null,
   UserID integer not null,
   ClientID integer not null,
   constraint pk_portaladminuseradditionalclients primary key (ID),
   constraint ak_portaladminuseradditionalclients1 unique (UserID,ClientID)
);

/* public.PortalClient */
drop table if exists public.PortalClient cascade;

create table public.PortalClient
(
   ID serial not null,
   GlobalID uuid not null default uuidv7(),
   Name text not null,
   Code text not null,
   Active char(1) not null default 'Y',
   ParentClientID integer,
   DatabaseID integer,
   KioskURL text,
   License bytea,
   ValidPattern text,
   ValidDescription text,
   Duration smallint,
   NumberPrior smallint,
   MaxAttempts smallint,
   AccessURL text,
   RedirectURL text,
   LDAPID integer,
   SAMLID integer,
   PasswordRulesID integer,
   ImpersonationEnabled char(1) not null default 'N',
   Type domain_type not null default 'UNIPORTAL',
   ExpirationWarning smallint,
   InviteLoginClientID integer,
   ManagerInviteLoginClientID integer,
   AutomationApiSecret text,
   constraint pk_portalclient primary key (ID),
   constraint ak_portalclient1 unique (GlobalID),
   constraint ak_portalclient2 unique (Name),
   constraint ck_portalcli_active_AFB1450E check(Active in ('Y','N')),
   constraint ck_portalcli_impersona_D4186AAD check(ImpersonationEnabled in ('Y','N')),
   constraint ck_portalcli_expiratio_F18A7705 check(ExpirationWarning >= 1)
);

/* public.PortalProperty */
drop table if exists public.PortalProperty cascade;

create table public.PortalProperty
(
   ID uuid not null default uuidv7(),
   PortalClientID integer not null,
   Name text not null,
   Code text not null,
   Active char(1) not null default 'Y',
   License text not null,
   TimeZoneID text not null,
   DefaultLocale text not null default 'en-US',
   TapsDatabaseID integer,
   PropertyType property_type,
   MarketType market_type,
   BrandID integer,
   NumberRooms integer,
   AddressLine1 text not null,
   AddressLine2 text,
   Locality text not null,
   AdministrativeArea text not null,
   PostalCode text not null,
   Country text not null,
   ClientRegionID integer,
   Unionized char(1) default 'N',
   OpsDateFormat text not null default 'MM/dd/yyyy',
   LogoReference text,
   constraint pk_portalproperty primary key (ID),
   constraint ak_portalproperty1 unique (PortalClientID,Name),
   constraint ak_portalproperty2 unique (PortalClientID,Code),
   constraint ck_portalpro_active_1D4166C4 check(Active in ('Y','N')),
   constraint ck_portalpro_unionized_4DC70061 check(Unionized in ('Y','N'))
);

/* public.PortalPropertyAdditionalLocale */
drop table if exists public.PortalPropertyAdditionalLocale cascade;

create table public.PortalPropertyAdditionalLocale
(
   ID uuid not null default uuidv7(),
   PortalPropertyID uuid not null,
   Locale text not null,
   constraint pk_portalpropertyadditionallocale primary key (ID),
   constraint ak_portalpropertyadditionallocale1 unique (PortalPropertyID,Locale)
);

/* public.PortalUser */
drop table if exists public.PortalUser cascade;

create table public.PortalUser
(
   ID serial not null,
   LoginClientID integer,
   LoginId text not null,
   GlobalID uuid not null default uuidv7(),
   PWord text not null,
   Active char(1) default 'N',
   ActiveChangedOn timestamp,
   FirstName text,
   MiddleName text,
   LastName text,
   EMail text,
   MobilePhone text,
   QuestionID integer,
   Answer text,
   LastLogin timestamp,
   PwordChanged timestamp,
   PwordChangedSameDayCount integer,
   Locale text,
   ClientID integer,
   DefaultDatabaseID integer,
   BypassLdap char(1) not null default 'N',
   LdapUserName text,
   Flags integer,
   Photo bytea,
   CreatedOn timestamp,
   CreatedByPortalUserID integer,
   CanAdministerUsers char(1) not null default 'N',
   constraint pk_portaluser primary key (ID),
   constraint ak_portaluser1 unique (LoginClientID,LoginId),
   constraint ak_portaluser2 unique (GlobalID),
   constraint ck_portaluse_active_EAC5020E check(Active in ('Y','N')),
   constraint ck_portaluse_bypasslda_84871397 check(BypassLdap in ('Y','N')),
   constraint ck_portaluse_canadmini_7918778 check(CanAdministerUsers in ('Y','N'))
);

create index ix_portaluser1 on public.PortalUser (EMail);

/* public.PortalUserAudit */
drop table if exists public.PortalUserAudit cascade;

create table public.PortalUserAudit
(
   ID bigserial not null,
   PortalUserID integer not null,
   LoginClientID integer,
   LoginId text not null,
   PWord text not null,
   Active char(1) default 'N',
   FirstName text,
   MiddleName text,
   LastName text,
   EMail text,
   MobilePhone text,
   QuestionID integer,
   Answer text,
   LastLogin timestamp,
   PwordChanged timestamp,
   Locale text,
   ClientID integer,
   DefaultDatabaseID integer,
   BypassLdap char(1) not null default 'N',
   LdapUserName text,
   Flags integer,
   ChangedOn timestamp not null,
   Operation char(1) not null,
   Command text not null,
   HostName text not null,
   ChangedBySqlUser text not null,
   ChangedByPortalUserID integer,
   CanAdministerUsers char(1) default 'N',
   constraint pk_portaluseraudit primary key (ID),
   constraint ck_portaluse_active_855DA7A1 check(Active in ('Y','N')),
   constraint ck_portaluse_bypasslda_305CB7AA check(BypassLdap in ('Y','N')),
   constraint ck_portaluse_canadmini_E697688B check(CanAdministerUsers in ('Y','N'))
);

create index ix_portaluseraudit1 on public.PortalUserAudit (LoginId,ChangedOn);
create index ix_portaluseraudit2 on public.PortalUserAudit (ChangedOn,LoginId);

/* public.PortalUserPropertyAccess */
drop table if exists public.PortalUserPropertyAccess cascade;

create table public.PortalUserPropertyAccess
(
   ID uuid not null default uuidv7(),
   PortalUserID integer not null,
   PortalPropertyID uuid not null,
   TapsEnabled char(1) not null default 'N',
   OpsEnabled char(1) not null default 'N',
   SurveyEnabled char(1) not null default 'N',
   TapsAdmin char(1) not null default 'N',
   constraint pk_portaluserpropertyaccess primary key (ID),
   constraint ak_portaluserpropertyaccess1 unique (PortalUserID,PortalPropertyID),
   constraint ck_portaluse_tapsenabl_C711A7C2 check(TapsEnabled in ('Y','N')),
   constraint ck_portaluse_opsenable_D6D18E7E check(OpsEnabled in ('Y','N')),
   constraint ck_portaluse_surveyena_596E2A18 check(SurveyEnabled in ('Y','N')),
   constraint ck_portaluse_tapsadmin_F3B74C10 check(TapsAdmin in ('Y','N'))
);

/* public.PortalUserRegistration */
drop table if exists public.PortalUserRegistration cascade;

create table public.PortalUserRegistration
(
   ID bigserial not null,
   PortalUserID integer not null,
   ExpiresOn timestamp not null,
   RegisteredOn timestamp,
   InvitedOn timestamp not null,
   InvitedByPortalUserID integer not null,
   NumberVerifyFailures integer not null default 0,
   VerificationCode integer,
   constraint pk_portaluserregistration primary key (ID),
   constraint ak_portaluserregistration1 unique (PortalUserID)
);

/* public.Product */
drop table if exists public.Product cascade;

create table public.Product
(
   ID serial not null,
   Code text not null,
   Name text not null,
   URL text,
   RequiredUserType required_user_type,
   RequiredDeviceTypes text,
   constraint pk_product primary key (ID),
   constraint ak_product1 unique (Code),
   constraint ak_product2 unique (Name)
);

/* public.ProtectedApiAccessKey */
drop table if exists public.ProtectedApiAccessKey cascade;

create table public.ProtectedApiAccessKey
(
   AccessKey text not null,
   constraint pk_protectedapiaccesskey primary key (AccessKey)
);

/* public.RemoteServiceProvider */
drop table if exists public.RemoteServiceProvider cascade;

create table public.RemoteServiceProvider
(
   Name text not null,
   DatabaseID integer,
   URL text not null,
   constraint pk_remoteserviceprovider primary key (Name,DatabaseID)
);

/* public.SAMLConfig */
drop table if exists public.SAMLConfig cascade;

create table public.SAMLConfig
(
   ID serial not null,
   URL text not null,
   SignatureMethodName text,
   SignatureMethodUri text,
   DigestMethod text,
   ProtocolBinding text,
   SpNameQualifier text,
   AuthnContextClassref text,
   NameidPolicyFormat text,
   KeystoreAlias text,
   UsernameVariable text,
   EmailVariable text,
   PropertyCodeVariable text,
   DisplayNameVariable text,
   EmployeeIdVariable text,
   EnableAutoRegistration char(1) not null default 'N',
   SignRequestKeystoreAlias text,
   SignResponseKeystoreAlias text,
   DecryptResponseKeystoreAlias text,
   UserAutoMigrationType text,
   AutoLinkEmployeeForManagersType text,
   UseAltEmpID char(1) not null default 'N',
   KeepMeSignedInDeviceTypes text,
   LogOffUrl text,
   ForceAuthn char(1) default 'N',
   constraint pk_samlconfig primary key (ID),
   constraint ck_samlconfi_enableaut_F1EA9BBB check(EnableAutoRegistration in ('Y','N')),
   constraint ck_samlconfi_usealtemp_87436811 check(UseAltEmpID in ('Y','N')),
   constraint ck_samlconfi_forceauth_BBF0840B check(ForceAuthn in ('Y','N'))
);

/* public.SAMLIDPConfig */
drop table if exists public.SAMLIDPConfig cascade;

create table public.SAMLIDPConfig
(
   ID serial not null,
   PortalClientID integer not null,
   ExternalProductName text,
   ExternalProductCode text,
   SAMLVersion text not null default 2.0,
   PostbackURL text not null,
   UserNameVariable text not null default 'Username',
   KeystoreAlias text,
   SignRequestKeystoreAlias text,
   SignResponseKeystoreAlias text,
   DecryptResponseKeystoreAlias text,
   constraint pk_samlidpconfig primary key (ID),
   constraint ak_samlidpconfig1 unique (PortalClientID,ExternalProductCode)
);

/* public.Session */
drop table if exists public.Session cascade;

create table public.Session
(
   ID serial not null,
   SessionID text not null,
   Host text not null,
   SessionTime timestamp not null,
   constraint pk_session primary key (ID),
   constraint ak_session1 unique (SessionID)
);

create index ix_session1 on public.Session (Host);

/* public.SessionHistory */
drop table if exists public.SessionHistory cascade;

create table public.SessionHistory
(
   ID serial not null,
   SessionID text not null,
   Host text not null,
   LoginID text,
   LoginDT timestamp,
   LogoutDT timestamp,
   TimedOut char(1) default 'N',
   DatabaseID integer,
   DatabaseName text,
   PropertyID integer,
   PropertyName text,
   constraint pk_sessionhistory primary key (ID),
   constraint ak_sessionhistory1 unique (SessionID),
   constraint ck_sessionhi_timedout_9051298 check(TimedOut in ('Y','N'))
);

create index ix_sessionhistory1 on public.SessionHistory (Host);
create index ix_sessionhistory2 on public.SessionHistory (LoginID,LoginDT);

/* public.SystemMessage */
drop table if exists public.SystemMessage cascade;

create table public.SystemMessage
(
   ID serial not null,
   StartTime timestamp not null,
   EndTime timestamp not null,
   MsgText text,
   constraint pk_systemmessage primary key (ID)
);

create index ix_systemmessage1 on public.SystemMessage (StartTime,EndTime);

/* public.TermsAndConditionsTextOverride */
drop table if exists public.TermsAndConditionsTextOverride cascade;

create table public.TermsAndConditionsTextOverride
(
   ClientID integer not null,
   Locale text not null,
   DisplayText text not null,
   constraint pk_termsandconditionstextoverride primary key (ClientID,Locale)
);

/* public.URLConfig */
drop table if exists public.URLConfig cascade;

create table public.URLConfig
(
   Name text not null,
   URL text not null,
   constraint pk_urlconfig primary key (Name)
);

/* public.UserData */
drop table if exists public.UserData cascade;

create table public.UserData
(
   ID serial not null,
   UserID integer not null,
   Tag text not null,
   TagValue text,
   TagData bytea,
   constraint pk_userdata primary key (ID),
   constraint ak_userdata1 unique (UserID,Tag)
);

/* public.UserDatabase */
drop table if exists public.UserDatabase cascade;

create table public.UserDatabase
(
   UserID integer not null,
   DatabaseID integer not null,
   constraint pk_userdatabase primary key (UserID,DatabaseID)
);

/* public.UserFailedPassword */
drop table if exists public.UserFailedPassword cascade;

create table public.UserFailedPassword
(
   ID serial not null,
   UserID integer not null,
   AttemptedOn timestamp not null,
   PWord text not null,
   constraint pk_userfailedpassword primary key (ID),
   constraint ak_userfailedpassword1 unique (UserID,AttemptedOn)
);

/* public.UserPreference */
drop table if exists public.UserPreference cascade;

create table public.UserPreference
(
   ID bigserial not null,
   UserID integer not null,
   PrefKey text not null,
   PrefValue text not null,
   constraint pk_userpreference primary key (ID),
   constraint ak_userpreference1 unique (UserID,PrefKey)
);

/* public.UserPriorPassword */
drop table if exists public.UserPriorPassword cascade;

create table public.UserPriorPassword
(
   ID serial not null,
   UserID integer not null,
   EffDate timestamp not null,
   PWord text not null,
   constraint pk_userpriorpassword primary key (ID),
   constraint ak_userpriorpassword1 unique (UserID,EffDate)
);

/* public.UserRefreshToken */
drop table if exists public.UserRefreshToken cascade;

create table public.UserRefreshToken
(
   UserID integer not null,
   RefreshToken text not null,
   constraint pk_userrefreshtoken primary key (UserID)
);

/* public.UserRegistration */
drop table if exists public.UserRegistration cascade;

create table public.UserRegistration
(
   ID bigserial not null,
   LoginID text not null,
   EMail text,
   ClientID integer not null,
   LoginClientID integer,
   DatabaseID integer not null,
   PropertyID integer not null,
   EmployeeID integer not null,
   EmpID text not null,
   FirstName text not null,
   MiddleName text not null,
   LastName text not null,
   ExpiresOn timestamp not null,
   RegisteredOn timestamp,
   OptOut char(1) not null default 'N',
   NumberVerifyFailures integer not null default 0,
   NumberOfAttempts integer not null default 0,
   Locale text,
   VerificationCode integer,
   CreatedByPortalUserID integer,
   UpdatedByPortalUserID integer,
   constraint pk_userregistration primary key (ID),
   constraint ak_userregistration1 unique (LoginID,ClientID,DatabaseID,PropertyID,EmployeeID),
   constraint ck_userregis_optout_63E35456 check(OptOut in ('Y','N'))
);

/* public.WatsonDatabase */
drop table if exists public.WatsonDatabase cascade;

create table public.WatsonDatabase
(
   ID serial not null,
   Name text not null,
   DatabaseURL text not null,
   DatabaseUserName text not null,
   DatabasePassword text not null,
   DatabaseDriver text not null,
   RedirectURL text,
   MobileRedirectURL text,
   ApiURL text,
   LoginURL text,
   WatsonVersion text,
   OlapDatabaseURL text,
   OlapDatabaseUserName text,
   OlapDatabasePassword text,
   constraint pk_watsondatabase primary key (ID),
   constraint ak_watsondatabase1 unique (Name)
);

/* relations */
alter table public.AccessURL add constraint fk_accessurl1 foreign key (PortalClientID) references public.PortalClient(ID) on delete no action;
alter table public.ApiAuthorization add constraint fk_apiauthorization1 foreign key (PortalUserID) references public.PortalUser(ID) on delete cascade;
alter table public.AutomationAuthorization add constraint fk_automationauthorization1 foreign key (PortalUserID) references public.PortalUser(ID) on delete cascade;
alter table public.ClientPreference add constraint fk_clientpreference1 foreign key (PortalClientID) references public.PortalClient(ID) on delete cascade;
alter table public.ClientProduct add constraint fk_clientproduct1 foreign key (ClientID) references public.PortalClient(ID) on delete cascade;
alter table public.ClientProduct add constraint fk_clientproduct2 foreign key (ProductID) references public.Product(ID) on delete cascade;
alter table public.ClientRegion add constraint fk_clientregion1 foreign key (PortalClientID) references public.PortalClient(ID) on delete cascade;
alter table public.ImpersonateHistory add constraint fk_impersonatehistory1 foreign key (PortalUserIDToImpersonate) references public.PortalUser(ID) on delete cascade;
alter table public.ImpersonateHistory add constraint fk_impersonatehistory2 foreign key (PortalUserIDOfImpersonator) references public.PortalUser(ID) on delete cascade;
alter table public.LoginHistory add constraint fk_loginhistory1 foreign key (PortalUserID) references public.PortalUser(ID) on delete cascade;
alter table public.LoginHistory add constraint fk_loginhistory2 foreign key (DatabaseID) references public.WatsonDatabase(ID) on delete cascade;
alter table public.NewUserRegistration add constraint fk_newuserregistration1 foreign key (ClientID) references public.PortalClient(ID) on delete cascade;
alter table public.NewUserRegistration add constraint fk_newuserregistration2 foreign key (LoginClientID) references public.PortalClient(ID) on delete cascade;
alter table public.NewUserRegistration add constraint fk_newuserregistration3 foreign key (CreatedByPortalUserID) references public.PortalUser(ID) on delete cascade;
alter table public.NewUserRegistration add constraint fk_newuserregistration4 foreign key (UpdatedByPortalUserID) references public.PortalUser(ID) on delete cascade;
alter table public.NewUserRegistration add constraint fk_newuserregistration5 foreign key (DatabaseID) references public.WatsonDatabase(ID) on delete cascade;
alter table public.NewUserRegistration add constraint fk_newuserregistration6 foreign key (PortalUserID) references public.PortalUser(ID) on delete cascade;
alter table public.PortalAdminUserAdditionalClients add constraint fk_portaladminuseradditionalclients1 foreign key (UserID) references public.PortalUser(ID) on delete cascade;
alter table public.PortalAdminUserAdditionalClients add constraint fk_portaladminuseradditionalclients2 foreign key (ClientID) references public.PortalClient(ID) on delete cascade;
alter table public.PortalClient add constraint fk_portalclient1 foreign key (ParentClientID) references public.PortalClient(ID) on delete cascade;
alter table public.PortalClient add constraint fk_portalclient2 foreign key (DatabaseID) references public.WatsonDatabase(ID) on delete set null;
alter table public.PortalClient add constraint fk_portalclient3 foreign key (PasswordRulesID) references public.PasswordRules(ID) on delete set null;
alter table public.PortalClient add constraint fk_portalclient4 foreign key (LDAPID) references public.LDAPConfig(ID) on delete set null;
alter table public.PortalClient add constraint fk_portalclient5 foreign key (SAMLID) references public.SAMLConfig(ID) on delete set null;
alter table public.PortalClient add constraint fk_portalclient6 foreign key (InviteLoginClientID) references public.PortalClient(ID) on delete cascade;
alter table public.PortalClient add constraint fk_portalclient7 foreign key (ManagerInviteLoginClientID) references public.PortalClient(ID) on delete cascade;
alter table public.PortalProperty add constraint fk_portalproperty1 foreign key (PortalClientID) references public.PortalClient(ID) on delete cascade;
alter table public.PortalProperty add constraint fk_portalproperty2 foreign key (TapsDatabaseID) references public.WatsonDatabase(ID) on delete set null;
alter table public.PortalProperty add constraint fk_portalproperty3 foreign key (BrandID) references public.Brand(ID) on delete set null;
alter table public.PortalProperty add constraint fk_portalproperty4 foreign key (ClientRegionID) references public.ClientRegion(ID) on delete set null;
alter table public.PortalPropertyAdditionalLocale add constraint fk_portalpropertyadditionallocale1 foreign key (PortalPropertyID) references public.PortalProperty(ID) on delete cascade;
alter table public.PortalUser add constraint fk_portaluser1 foreign key (QuestionID) references public.PasswordQuestion(ID) on delete no action;
alter table public.PortalUser add constraint fk_portaluser2 foreign key (LoginClientID) references public.PortalClient(ID) on delete no action;
alter table public.PortalUser add constraint fk_portaluser3 foreign key (ClientID) references public.PortalClient(ID) on delete no action;
alter table public.PortalUser add constraint fk_portaluser4 foreign key (DefaultDatabaseID) references public.WatsonDatabase(ID) on delete set null;
alter table public.PortalUser add constraint fk_portaluser5 foreign key (CreatedByPortalUserID) references public.PortalUser(ID) on delete set null;
alter table public.PortalUserPropertyAccess add constraint fk_portaluserpropertyaccess1 foreign key (PortalUserID) references public.PortalUser(ID) on delete cascade;
alter table public.PortalUserPropertyAccess add constraint fk_portaluserpropertyaccess2 foreign key (PortalPropertyID) references public.PortalProperty(ID) on delete cascade;
alter table public.PortalUserRegistration add constraint fk_portaluserregistration1 foreign key (PortalUserID) references public.PortalUser(ID) on delete cascade;
alter table public.PortalUserRegistration add constraint fk_portaluserregistration2 foreign key (InvitedByPortalUserID) references public.PortalUser(ID) on delete cascade;
alter table public.SAMLIDPConfig add constraint fk_samlidpconfig1 foreign key (PortalClientID) references public.PortalClient(ID) on delete cascade;
alter table public.TermsAndConditionsTextOverride add constraint fk_termsandconditionstextoverride1 foreign key (ClientID) references public.PortalClient(ID) on delete cascade;
alter table public.UserData add constraint fk_userdata1 foreign key (UserID) references public.PortalUser(ID) on delete cascade;
alter table public.UserDatabase add constraint fk_userdatabase1 foreign key (UserID) references public.PortalUser(ID) on delete cascade;
alter table public.UserDatabase add constraint fk_userdatabase2 foreign key (DatabaseID) references public.WatsonDatabase(ID) on delete cascade;
alter table public.UserFailedPassword add constraint fk_userfailedpassword1 foreign key (UserID) references public.PortalUser(ID) on delete cascade;
alter table public.UserPreference add constraint fk_userpreference1 foreign key (UserID) references public.PortalUser(ID) on delete cascade;
alter table public.UserPriorPassword add constraint fk_userpriorpassword1 foreign key (UserID) references public.PortalUser(ID) on delete cascade;
alter table public.UserRefreshToken add constraint fk_userrefreshtoken1 foreign key (UserID) references public.PortalUser(ID) on delete cascade;
alter table public.UserRegistration add constraint fk_userregistration1 foreign key (CreatedByPortalUserID) references public.PortalUser(ID) on delete cascade;
alter table public.UserRegistration add constraint fk_userregistration2 foreign key (UpdatedByPortalUserID) references public.PortalUser(ID) on delete cascade;

/* public.portaluser_delete */
create or replace function public.portaluser_delete() returns trigger as $BODY$
begin
declare
                  current_statement text := ( select query from pg_stat_activity where pid = pg_backend_pid() );
                  portal_user_id int;  -- TODO: Add support
               begin
                  insert into PortalUserAudit ( PortalUserID,
                                            LoginClientID,
                                            LoginId,
                                            PWord,
                                            Active,
                                            FirstName,
                                            MiddleName,
                                            LastName,
                                            EMail,
                                            MobilePhone,
                                            QuestionID,
                                            Answer,
                                            LastLogin,
                                            PwordChanged,
                                            Locale,
                                            ClientID,
                                            DefaultDatabaseID,
                                            BypassLDAP,
                                            LdapUserName,
                                            Flags,
                                            ChangedOn,
                                            Operation,
                                            Command,
                                            HostName,
                                            ChangedBySqlUser,
                                            ChangedByPortalUserID,
                                            CanAdministerUsers )
                      values (
                         OLD.ID,
                         OLD.LoginClientID,
                         OLD.LoginId,
                         OLD.PWord,
                         OLD.Active,
                         OLD.FirstName,
                         OLD.MiddleName,
                         OLD.LastName,
                         OLD.EMail,
                         OLD.MobilePhone,
                         OLD.QuestionID,
                         OLD.Answer,
                         OLD.LastLogin,
                         OLD.PwordChanged,
                         OLD.Locale,
                         OLD.ClientID,
                         OLD.DefaultDatabaseID,
                         OLD.BypassLDAP,
                         OLD.LdapUserName,
                         OLD.Flags,
                         timezone('utc', now()),
                         'D',
                         current_statement,
                         inet_client_addr(),
                         CURRENT_USER,
                         portal_user_id,
                         OLD.CanAdministerUsers );
               end;
   return null;
end;
$BODY$ language plpgsql;

drop trigger if exists portaluser_delete on public.PortalUser cascade;
create trigger portaluser_delete after delete on public.PortalUser
   for each row execute procedure public.portaluser_delete();

/* public.portaluser_update */
create or replace function public.portaluser_update() returns trigger as $BODY$
begin
declare
                  operation char(1) := (case when tg_op = 'INSERT' then 'I' else 'U' end);
                  current_statement text := ( select query from pg_stat_activity where pid = pg_backend_pid() );
                  portal_user_id int;  -- TODO: Add support
               begin
                  if operation = 'I' then
                     insert into PortalUserAudit (PortalUserID,
                                               LoginClientID,
                                               LoginId,
                                               PWord,
                                               Active,
                                               FirstName,
                                               MiddleName,
                                               LastName,
                                               EMail,
                                               MobilePhone,
                                               QuestionID,
                                               Answer,
                                               LastLogin,
                                               PwordChanged,
                                               Locale,
                                               ClientID,
                                               DefaultDatabaseID,
                                               BypassLDAP,
                                               LdapUserName,
                                               Flags,
                                               ChangedOn,
                                               Operation,
                                               Command,
                                               HostName,
                                               ChangedBySqlUser,
                                               ChangedByPortalUserID,
                                               CanAdministerUsers )
                            values (
                               NEW.ID,
                               NEW.LoginClientID,
                               NEW.LoginId,
                               NEW.PWord,
                               NEW.Active,
                               NEW.FirstName,
                               NEW.MiddleName,
                               NEW.LastName,
                               NEW.EMail,
                               NEW.MobilePhone,
                               NEW.QuestionID,
                               NEW.Answer,
                               NEW.LastLogin,
                               NEW.PwordChanged,
                               NEW.Locale,
                               NEW.ClientID,
                               NEW.DefaultDatabaseID,
                               NEW.BypassLDAP,
                               NEW.LdapUserName,
                               NEW.Flags,
                               timezone('utc', now()),
                               operation,
                               current_statement,
                               inet_client_addr(),
                               CURRENT_USER,
                               portal_user_id,
                               NEW.CanAdministerUsers );
                  elseif NEW.ID = OLD.ID and
                         NEW.LastLogin IS NOT DISTINCT FROM OLD.LastLogin and (
                           COALESCE(NEW.PWord, '') <> COALESCE(OLD.PWord, '') or
                           COALESCE(NEW.EMail, '') <> COALESCE(OLD.EMail, '') or
                           COALESCE(NEW.Flags, 0) <> COALESCE(OLD.Flags, 0) or
                           COALESCE(NEW.Active, '') <> COALESCE(OLD.Active, '') or
                           COALESCE(NEW.QuestionID, 0) <> COALESCE(OLD.QuestionID, 0) or
                           COALESCE(NEW.Locale, '') <> COALESCE(OLD.Locale, '') or
                           COALESCE(NEW.ClientID, 0) <> COALESCE(OLD.ClientID, 0) or
                           COALESCE(NEW.DefaultDatabaseID, 0) <> COALESCE(OLD.DefaultDatabaseID, 0) or
                           COALESCE(NEW.BypassLdap, '') <> COALESCE(OLD.BypassLdap, '') or
                           COALESCE(NEW.LoginClientID, 0) <> COALESCE(OLD.LoginClientID, 0) or
                           COALESCE(NEW.MobilePhone, '') <> COALESCE(OLD.MobilePhone, '') or
                           COALESCE(NEW.FirstName, '') <> COALESCE(OLD.FirstName, '') or
                           COALESCE(NEW.MiddleName, '') <> COALESCE(OLD.MiddleName, '') or
                           COALESCE(NEW.LastName, '') <> COALESCE(OLD.LastName, '') or
                           COALESCE(NEW.LdapUserName, '') <> COALESCE(OLD.LdapUserName, '') or
                           COALESCE(NEW.PwordChangedSameDayCount, 0) <> COALESCE(OLD.PwordChangedSameDayCount, 0) ) then
                     insert into PortalUserAudit (PortalUserID,
                                               LoginClientID,
                                               LoginId,
                                               PWord,
                                               Active,
                                               FirstName,
                                               MiddleName,
                                               LastName,
                                               EMail,
                                               MobilePhone,
                                               QuestionID,
                                               Answer,
                                               LastLogin,
                                               PwordChanged,
                                               Locale,
                                               ClientID,
                                               DefaultDatabaseID,
                                               BypassLDAP,
                                               LdapUserName,
                                               Flags,
                                               ChangedOn,
                                               Operation,
                                               Command,
                                               HostName,
                                               ChangedBySqlUser,
                                               ChangedByPortalUserID,
                                               CanAdministerUsers)
                         values (
                            NEW.ID,
                            NEW.LoginClientID,
                            NEW.LoginId,
                            NEW.PWord,
                            NEW.Active,
                            NEW.FirstName,
                            NEW.MiddleName,
                            NEW.LastName,
                            NEW.EMail,
                            NEW.MobilePhone,
                            NEW.QuestionID,
                            NEW.Answer,
                            NEW.LastLogin,
                            NEW.PwordChanged,
                            NEW.Locale,
                            NEW.ClientID,
                            NEW.DefaultDatabaseID,
                            NEW.BypassLDAP,
                            NEW.LdapUserName,
                            NEW.Flags,
                            timezone('utc', now()),
                            operation,
                            current_statement,
                            inet_client_addr(),
                            CURRENT_USER,
                            portal_user_id,
                            NEW.CanAdministerUsers );
                  end if;
               end;
   return new;
end;
$BODY$ language plpgsql;

drop trigger if exists portaluser_update on public.PortalUser cascade;
create trigger portaluser_update after insert or update on public.PortalUser
   for each row execute procedure public.portaluser_update();

/* public.UserDatabaseQuery */
create or replace view public.UserDatabaseQuery as
   select u.ID as UserID,u.LoginId as LoginId,u.EMail,u.Flags,d.ID as DatabaseID,d.Name as DatabaseName,d.DatabaseURL,d.DatabaseUserName,d.DatabasePassword,d.DatabaseDriver,d.RedirectURL from PortalUser u,WatsonDatabase d,UserDatabase ud where u.ID = ud.UserID and d.ID = ud.DatabaseID;

