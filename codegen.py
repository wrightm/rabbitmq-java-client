##   The contents of this file are subject to the Mozilla Public License
##   Version 1.1 (the "License"); you may not use this file except in
##   compliance with the License. You may obtain a copy of the License at
##   http://www.mozilla.org/MPL/
##
##   Software distributed under the License is distributed on an "AS IS"
##   basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
##   License for the specific language governing rights and limitations
##   under the License.
##
##   The Original Code is RabbitMQ.
##
##   The Initial Developers of the Original Code are LShift Ltd,
##   Cohesive Financial Technologies LLC, and Rabbit Technologies Ltd.
##
##   Portions created before 22-Nov-2008 00:00:00 GMT by LShift Ltd,
##   Cohesive Financial Technologies LLC, or Rabbit Technologies Ltd
##   are Copyright (C) 2007-2008 LShift Ltd, Cohesive Financial
##   Technologies LLC, and Rabbit Technologies Ltd.
##
##   Portions created by LShift Ltd are Copyright (C) 2007-2010 LShift
##   Ltd. Portions created by Cohesive Financial Technologies LLC are
##   Copyright (C) 2007-2010 Cohesive Financial Technologies
##   LLC. Portions created by Rabbit Technologies Ltd are Copyright
##   (C) 2007-2010 Rabbit Technologies Ltd.
##
##   All Rights Reserved.
##
##   Contributor(s): ______________________________________.
##

from __future__ import nested_scopes
import re
import sys

sys.path.append("../rabbitmq-codegen")  # in case we're next to an experimental revision
sys.path.append("codegen")              # in case we're building from a distribution package

from amqp_codegen import *

class BogusDefaultValue(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)


def java_constant_name(c):
    return '_'.join(re.split('[- ]', c.upper()))

javaTypeMap = {
    'octet': 'int',
    'shortstr': 'java.lang.String',
    'longstr': 'LongString',
    'short': 'int',
    'long': 'int',
    'longlong': 'long',
    'bit': 'boolean',
    'table': 'Map<java.lang.String,Object>',
    'timestamp': 'Date'
    }

javaTypesNeverNullInBuilder = set([
    'java.lang.String',
    'LongString',
    'Map<java.lang.String,Object>',
    'Date'
    ])

javaPropertyTypeMap = {
    'octet': 'java.lang.Integer',
    'shortstr': 'java.lang.String',
    'longstr': 'LongString',
    'short': 'java.lang.Integer',
    'long': 'java.lang.Integer',
    'longlong': 'java.lang.Long',
    'bit': 'java.lang.Boolean',
    'table': 'Map<java.lang.String,Object>',
    'timestamp': 'Date'
    }

def java_type(spec, domain):
    return javaTypeMap[spec.resolveDomain(domain)]

def java_name(upper, name):
    out = ''
    for c in name:
        if not c.isalnum():
            upper = True
        elif upper:
            out += c.upper()
            upper = False
        else:
            out += c
    return out

def java_class_name(name):
    return java_name(True, name)

def java_getter_name(name):
    return java_name(False, 'get-' + name)

def java_property_type(spec, type):
    return javaPropertyTypeMap[spec.resolveDomain(type)]
def java_field_name(name):
    return java_name(False, name)
def java_field_type(spec, domain):
    return javaTypeMap[spec.resolveDomain(domain)]

def java_field_default_value(type, value):
    if type == 'int':
        return value
    elif type == 'boolean':
        return "{0}".format(value).lower()
    elif type == 'java.lang.String':
        return "\"{0}\"".format(value)
    elif type == 'LongString':
        return "new LongStringHelper.ByteArrayLongString(\"{0}\".getBytes());".format(value)
    elif type == 'long':
        return "{0}L".format(value)
    elif type == 'Map<java.lang.String,Object>':
        return "new HashMap<java.lang.String,Object>()"
    else:
        raise BogusDefaultValue("JSON provided default value {0} for suspicious type {1}".format(value, type))

def typeNameDefault(spec, a):
    return (java_field_type(spec, a.domain),
            java_field_name(a.name),
            java_field_default_value(java_field_type(spec, a.domain),
                                     a.defaultvalue))

def mandatoryAndNullCheckedFields(spec, m):
    fieldsToNullCheckInBuild = set([])
    mandatoryFields          = set([])
    if m.arguments:
        for index, a in enumerate(m.arguments):
            (jfType, jfName, jfDefault) = typeNameDefault(spec,a)
            if jfType in javaTypesNeverNullInBuilder:
                fieldsToNullCheckInBuild.update([jfName])
            if a.defaultvalue == None:
                mandatoryFields.update([(jfType,jfName)])
    return (fieldsToNullCheckInBuild, mandatoryFields)

def builderCtorArgSignature(mandatoryFields):
    ctor_arg_signature_list = []
    ctor_arg_signature_string = ", ".join(ctor_arg_signature_list)
    return ctor_arg_signature_string

#---------------------------------------------------------------------------

def printFileHeader():
    print """//   NOTE: This -*- java -*- source code is autogenerated from the AMQP
//         specification!
//
//   The contents of this file are subject to the Mozilla Public License
//   Version 1.1 (the "License"); you may not use this file except in
//   compliance with the License. You may obtain a copy of the License at
//   http://www.mozilla.org/MPL/
//
//   Software distributed under the License is distributed on an "AS IS"
//   basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//   License for the specific language governing rights and limitations
//   under the License.
//
//   The Original Code is RabbitMQ.
//
//   The Initial Developers of the Original Code are LShift Ltd,
//   Cohesive Financial Technologies LLC, and Rabbit Technologies Ltd.
//
//   Portions created before 22-Nov-2008 00:00:00 GMT by LShift Ltd,
//   Cohesive Financial Technologies LLC, or Rabbit Technologies Ltd
//   are Copyright (C) 2007-2008 LShift Ltd, Cohesive Financial
//   Technologies LLC, and Rabbit Technologies Ltd.
//
//   Portions created by LShift Ltd are Copyright (C) 2007-2010 LShift
//   Ltd. Portions created by Cohesive Financial Technologies LLC are
//   Copyright (C) 2007-2010 Cohesive Financial Technologies
//   LLC. Portions created by Rabbit Technologies Ltd are Copyright
//   (C) 2007-2010 Rabbit Technologies Ltd.
//
//   All Rights Reserved.
//
//   Contributor(s): ______________________________________.
//
"""

def genJavaApi(spec):
    def printHeader():
        printFileHeader()
        print """package com.rabbitmq.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

import com.rabbitmq.client.impl.ContentHeaderPropertyWriter;
import com.rabbitmq.client.impl.ContentHeaderPropertyReader;
import com.rabbitmq.client.impl.LongString;
import com.rabbitmq.client.impl.LongStringHelper;

public interface AMQP
{
    public static class PROTOCOL {"""
        print "        public static final int MAJOR = %i;" % spec.major
        print "        public static final int MINOR = %i;" % spec.minor
        print "        public static final int REVISION = %i;" % spec.revision
        print "        public static final int PORT = %i;" % spec.port
        print "    }"

    def printConstants():
        print
        for (c,v,cls) in spec.constants: print "    public static final int %s = %i;" % (java_constant_name(c), v)

    def builder(c,m):
        def ctorCall(c,m):
            ctor_call = "return new com.rabbitmq.client.impl.AMQImpl.%s.%s(" % (java_class_name(c.name),java_class_name(m.name))
            ctor_arg_list = []
            if m.arguments:
                for index, a in enumerate(m.arguments):
                    ctor_arg_list.append("{0}".format(java_field_name(a.name)))
            ctor_call += ", ".join(ctor_arg_list)
            ctor_call += ");"
            print "                     %s" % (ctor_call)

        def genFields(spec, m):
            (fieldsToNullCheckInBuild, mandatoryFields) = mandatoryAndNullCheckedFields(spec, m)
            if m.arguments:
                for index, a in enumerate(m.arguments):
                    (jfType, jfName, jfDefault) = typeNameDefault(spec, a)
                    if a.defaultvalue != None:
                        print "                private %s %s = %s;" % (jfType, jfName, jfDefault)
                    else:
                        print "                private %s %s;" % (jfType, jfName)

        def genBuilderCtor(m, mandatoryFields):
            ctor_arg_signature_string = builderCtorArgSignature(mandatoryFields)
            print "                public Builder(%s)" % ctor_arg_signature_string
            print "                { }"

        def genArgMethods(spec, m):
            if m.arguments:
                for index, a in enumerate(m.arguments):
                    (jfType, jfName, jfDefault) = typeNameDefault(spec, a)
                    print "                public Builder %s(%s %s)" % (jfName, jfType, jfName)
                    print "                    { this.%s = %s;      return this; }" % (jfName, jfName)
                    if jfType == "boolean":
                        print "                public Builder %s()" % (jfName)
                        print "                    { this.%s = true;      return this; }" % (jfName)

        def genBuildMethod(c,m,fieldsToNullCheckInBuild):
            print "                public %s build()" % (java_class_name(m.name))
            print "                {"

            if len(fieldsToNullCheckInBuild) != 0:
                nullCheckClauses = []
                for f in fieldsToNullCheckInBuild:
                    nullCheckClauses.append("{0} == null".format(f))
                nullCheckClause = " ||\n                       ".join(nullCheckClauses) + ")"
                print "                    if(%s" % (nullCheckClause)
                print "                    {"
                print "                        throw new IllegalStateException(\"Invalid configuration.\");"
                print "                    }"
                print
            ctorCall(c,m)
            print "                }"

        print
        print "            // Builder for instances of %s.%s" % (java_class_name(c.name), java_class_name(m.name))
        print "            public static class Builder"
        print "            {"
        (fieldsToNullCheckInBuild, mandatoryFields) = mandatoryAndNullCheckedFields(spec, m)
        genFields(spec, m)
        print
        genBuilderCtor(m, mandatoryFields)
        print
        genArgMethods(spec, m)
        print
        genBuildMethod(c,m,fieldsToNullCheckInBuild)
        print "            }"

    def genBuilderGetterApi(c):
        print
        print "    // Convenience getters for AMQP method builders"
        for m in c.allMethods():
            (cName, mName) = (java_class_name(c.name), java_class_name(m.name))
            (fieldsToNullCheck, mandatoryFields) = mandatoryAndNullCheckedFields(spec, m)
            argSignature = builderCtorArgSignature(mandatoryFields)
            print "    public %s.%s.Builder %s%s(%s);" % (cName, mName, cName.lower(), mName, argSignature)

    def printClassInterfaces():
        for c in spec.classes:
            # genBuilderGetterApi(c)
            print
            print "    public static class %s {" % (java_class_name(c.name))
            for m in c.allMethods():
                print "        public interface %s extends Method {" % ((java_class_name(m.name)))
                for a in m.arguments:
                    print "            %s %s();" % (java_field_type(spec, a.domain), java_getter_name(a.name))
                builder(c,m)
                print "        }"
            print "    }"

    def printReadProperties(c):
        print
        print """        public void readPropertiesFrom(ContentHeaderPropertyReader reader)
            throws IOException
        {"""
        for f in c.fields:
            print "            boolean %s_present = reader.readPresence();" % (java_field_name(f.name))
        print "            reader.finishPresence();"
        for f in c.fields:
            print "            this.%s = %s_present ? reader.read%s() : null;" % (java_field_name(f.name), java_field_name(f.name),  java_class_name(f.domain))
        print "        }"

    def printWriteProperties(c):
        print
        print """        public void writePropertiesTo(ContentHeaderPropertyWriter writer)
            throws IOException
        {"""
        for f in c.fields:
            print "            writer.writePresence(this.%s != null);" % (java_field_name(f.name))
        print "            writer.finishPresence();"
        for f in c.fields:
            print "            if (this.%s != null) { writer.write%s(this.%s); } " % (java_field_name(f.name), java_class_name(f.domain), java_field_name(f.name))
        print "        }"

    def printPropertyDebug(c):
        print
        print "        public void appendPropertyDebugStringTo(StringBuffer acc) {"
        print "            acc.append(\"(\");"
        for index,f in enumerate(c.fields):
            print "            acc.append(\"%s=\");" % (f.name)
            print "            acc.append(this.%s);" % (java_field_name(f.name))
            if not index == len(c.fields) - 1:
                print "            acc.append(\", \");"

        print "            acc.append(\")\");"
        print "        }"

    def printClassProperties(c):
        print
        print "    public static class %(className)s extends %(parentClass)s {" % {'className' : java_class_name(c.name) + 'Properties', 'parentClass' : 'com.rabbitmq.client.impl.AMQ' + java_class_name(c.name) + 'Properties'}
        #property fields
        for f in c.fields:
            print "        private %s %s;" % (java_property_type(spec, f.domain),java_field_name(f.name))

        #constructor
        if c.fields:
            print
            print "        public %sProperties ( " % (java_class_name(c.name))
            for index,f in enumerate(c.fields):
                sys.stdout.write( "            %s %s" % (java_property_type(spec,f.domain),java_field_name(f.name)))
                if not index == len(c.fields) - 1:
                    print ","

            print ")"
            print "        {"
            for f in c.fields:
                print "            this.%s = %s;" % (java_field_name(f.name), java_field_name(f.name))
            print "        }"

        #empty constructor
        print
        print "        public %sProperties() {}" % (java_class_name(c.name))
        print "        public int getClassId() { return %i; }" % (c.index)
        print "        public java.lang.String getClassName() { return \"%s\"; }" % (c.name)

        #access functions
        print
        for f in c.fields:
            print """        public %(fieldType)s get%(capFieldName)s() { return %(fieldName)s; }
        public void set%(capFieldName)s(%(fieldType)s %(fieldName)s) { this.%(fieldName)s = %(fieldName)s; }""" % \
            {'fieldType' : java_property_type(spec, f.domain), \
            'capFieldName' : (java_field_name(f.name)[0].upper() + java_field_name(f.name)[1:]), \
            'fieldName' : java_field_name(f.name)}

        printReadProperties(c)
        printWriteProperties(c)
        printPropertyDebug(c)
        print "    }"

    printHeader()
    printConstants()
    printClassInterfaces()

    for c in spec.classes:
        if c.hasContentProperties:
            printClassProperties(c)
    print "}"

#--------------------------------------------------------------------------------

def genJavaImpl(spec):
    def printHeader():
        printFileHeader()
        print """package com.rabbitmq.client.impl;

import java.io.IOException;
import java.io.DataInputStream;
import java.util.Map;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.UnknownClassOrMethodId;
import com.rabbitmq.client.UnexpectedMethodError;

public class AMQImpl implements AMQP
{"""
    def genBuilderGetterImpl(spec,c):
        print
        print "    // Convenience getter methods for AMQP Method builders"
        for m in c.allMethods():
            (cName, mName) = (java_class_name(c.name), java_class_name(m.name))
            (fieldsToNullCheck, mandatoryFields) = mandatoryAndNullCheckedFields(spec, m)
            argSignature = builderCtorArgSignature(mandatoryFields)
            ctorArgList = []
            for (argType, argName) in mandatoryFields:
                ctorArgList.append(argName)
            print "    public static %s.%s.Builder %s%s(%s)" % (cName, mName, cName.lower(), mName, argSignature)
            print "    {"
            print "        return new %s.%s.Builder(%s);" % (cName, mName, ", ".join(ctorArgList))
            print "    }"
            print

    def printClassMethods(spec, c):
        # genBuilderGetterImpl(spec,c)
        print
        print "    public static class %s {" % (java_class_name(c.name))
        print "        public static final int INDEX = %s;" % (c.index)
        for m in c.allMethods():

            def getters():
                if m.arguments:
                    print
                    for a in m.arguments:
                        print "            public %s %s() { return %s; }" % (java_field_type(spec,a.domain), java_getter_name(a.name), java_field_name(a.name))

            def constructor():
                if m.arguments:
                    print
                    print "            public %s(" % (java_class_name(m.name))
                    for index,a in enumerate(m.arguments):
                        sys.stdout.write("                %s %s" % (java_field_type(spec,a.domain), java_field_name(a.name)))
                        if not index == len(m.arguments) - 1:
                            print ","
                    print ")"
                    print "            {"
                    for a in m.arguments:
                        print "                this.%s = %s;" % (java_field_name(a.name), java_field_name(a.name))
                    print "            }"

            def others():
                print
                print "            public %s() {}" % (java_class_name(m.name))
                print "            public int protocolClassId() { return %s; }" % (c.index)
                print "            public int protocolMethodId() { return %s; }" % (m.index)
                print "            public java.lang.String protocolMethodName() { return \"%s.%s\";}" % (c.name, m.name)
                print
                print "            public boolean hasContent() {"
                if m.hasContent:
                    print "                return true;"
                else:
                    print "                return false;"
                print "            }"

                print
                print """            public Object visit(MethodVisitor visitor) throws IOException {
                return visitor.visit(this);
            }"""

            def argument_debug_string():
                print
                print "            public void appendArgumentDebugStringTo(StringBuffer acc) {"
                print "                acc.append(\"(\");"
                for index, a in enumerate(m.arguments):
                    print "                acc.append(\"%s=\");" % (a.name)
                    print "                acc.append(this.%s);" % (java_field_name(a.name))
                    if not index == len(m.arguments) - 1:
                        print "                acc.append(\",\");"
                print "                acc.append(\")\");"
                print "            }"

            def read_arguments():
                print
                print "            public void readArgumentsFrom(MethodArgumentReader reader)"
                print "                throws IOException"
                print "            {"
                for a in m.arguments:
                    print "                this.%s = reader.read%s();" % (java_field_name(a.name), java_class_name(spec.resolveDomain(a.domain)))
                print "            }"

            def write_arguments():
                print
                print "            public void writeArgumentsTo(MethodArgumentWriter writer)"
                print "                throws IOException"
                print "            {"
                for a in m.arguments:
                    print "                writer.write%s(this.%s);" % (java_class_name(spec.resolveDomain(a.domain)), java_field_name(a.name))
                print "            }"

            #start
            print
            print "        public static class %s" % (java_class_name(m.name),)
            print "            extends Method"
            print "            implements com.rabbitmq.client.AMQP.%s.%s" % (java_class_name(c.name), java_class_name(m.name))
            print "        {"
            print "            public static final int INDEX = %s;" % (m.index)
            if m.arguments:
                print
                for a in m.arguments:
                    print "            public %s %s;" % (java_field_type(spec, a.domain), java_field_name(a.name))

            getters()
            constructor()
            others()

            argument_debug_string()
            read_arguments()
            write_arguments()

            print "        }"
        print "    }"

    def printMethodVisitor():
        print
        print "    public interface MethodVisitor {"
        for c in spec.allClasses():
            for m in c.allMethods():
                print "        Object visit(%s.%s x) throws IOException;" % (java_class_name(c.name), java_class_name(m.name))
        print "    }"

        #default method visitor
        print
        print "    public static class DefaultMethodVisitor implements MethodVisitor {"
        for c in spec.allClasses():
            for m in c.allMethods():
               print "        public Object visit(%s.%s x) throws IOException { throw new UnexpectedMethodError(x); } " % (java_class_name(c.name), java_class_name(m.name))
        print "    }"

    def printMethodArgumentReader():
        print
        print "    public static Method readMethodFrom(DataInputStream in) throws IOException { "
        print "        int classId = in.readShort();"
        print "        int methodId = in.readShort();"
        print "        switch (classId) {"
        for c in spec.allClasses():
            print "            case %s:" % (c.index)
            print "                switch (methodId) {"
            for m in c.allMethods():
                fq_name = java_class_name(c.name) + '.' + java_class_name(m.name)
                print "                    case %s: {" % (m.index)
                print "                        %s result = new %s();" % (fq_name, fq_name)
                print "                        result.readArgumentsFrom(new MethodArgumentReader(in));"
                print "                        return result;"
                print "                    }"
            print "                    default: break;"
            print "                }"
        print "        }"
        print
        print "        throw new UnknownClassOrMethodId(classId, methodId);"
        print "    }"

    def printContentHeaderReader(c):
        print
        print """    public static AMQContentHeader readContentHeaderFrom(DataInputStream in)
        throws IOException
    {
        int classId = in.readShort();

        switch (classId) {"""
        for c in spec.allClasses():
            if len(c.fields) > 0:
                print "            case %s: return new %sProperties();" %(c.index, (java_class_name(c.name)))
        print "            default: break;"
        print "        }"
        print
        print "        throw new UnknownClassOrMethodId(classId, -1);"
        print "    }"

    printHeader()
    for c in spec.allClasses(): printClassMethods(spec,c)
    printMethodVisitor()
    printMethodArgumentReader()
    printContentHeaderReader(c)
    print "}"

#--------------------------------------------------------------------------------

def generateJavaApi(specPath):
    genJavaApi(AmqpSpec(specPath))

def generateJavaImpl(specPath):
    genJavaImpl(AmqpSpec(specPath))

if __name__ == "__main__":
    do_main(generateJavaApi, generateJavaImpl)
