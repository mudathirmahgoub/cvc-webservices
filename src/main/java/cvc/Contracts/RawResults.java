/* This file is part of the cvc-webservices.
 *  Copyright (c) 2018 by the Board of Trustees of the University of Iowa
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License.  You
 *  may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package cvc.Contracts;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Results")
public class RawResults
{
    @XmlAttribute(name = "jobId")
    public String jobId;

    @XmlElement(name = "data")
    public String data;

    /*
     used by the web service to indicate job status.
     This field is not provided by cvc program
    */
    @XmlAttribute
    public boolean jobFinished;
}
