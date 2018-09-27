package cvc.Contracts;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Kind")
public class Kind
{
    @XmlAttribute(name = "name")
    public String name;

    @XmlElement(name = "Example")
    @JsonProperty("names")
    public String [] names;
}
