package com.raddle.comic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.w3c.dom.Document;

/**
 * 配置文件（view-config.xml、recent-view.xml）用的 XMLConfiguration。
 *
 * commons-configuration 1.x 自带的 save() 用 JDK 自带的恒等序列化器写文件，它会把标签之间的
 * 空白当成"空白文本节点"原样写回去，写的时候还把自己的缩进混进去——每存一次文件就多出一批
 * 空行，程序每启动一次文件就胖一圈。
 *
 * 这里改成：先拿到 DOM，把只含空白的文本节点删掉，再让同一个序列化器正常缩进输出。
 * 读的部分还是 commons-configuration 的 load()，取值不受影响。
 */
public class XmlConfig extends XMLConfiguration {

	private static final long serialVersionUID = 1L;

	/**
	 * 把配置写到文件。
	 */
	public void saveTo(File file) throws ConfigurationException, IOException {
		Document dom = createDocument();
		removeBlankText(dom);
		Writer writer = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty("indent", "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty("encoding", "UTF-8");
			transformer.transform(new DOMSource(dom), new StreamResult(writer));
		} catch (TransformerException e) {
			throw new IOException("写配置文件失败：" + file.getAbsolutePath(), e);
		} finally {
			writer.close();
		}
	}

	/**
	 * 删掉只含空白的文本节点。
	 *
	 * 只在父元素还有子元素的时候删——元素的值本身就是空白时要留着，
	 * 不然 &lt;comicName&gt;&lt;/comicName&gt; 会变成 &lt;comicName/&gt;，
	 * 读回来 null 和空字符串不一样。
	 */
	private static void removeBlankText(org.w3c.dom.Node node) {
		org.w3c.dom.NodeList children = node.getChildNodes();
		List<org.w3c.dom.Node> blankNodes = new ArrayList<org.w3c.dom.Node>();
		boolean hasElementChild = false;
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
				hasElementChild = true;
				break;
			}
		}
		if (hasElementChild) {
			for (int i = 0; i < children.getLength(); i++) {
				org.w3c.dom.Node child = children.item(i);
				if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE
						|| child.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) {
					String value = child.getNodeValue();
					if (value == null || value.trim().isEmpty()) {
						blankNodes.add(child);
					}
				}
			}
		}
		for (org.w3c.dom.Node blankNode : blankNodes) {
			node.removeChild(blankNode);
		}
		org.w3c.dom.NodeList rest = node.getChildNodes();
		for (int i = 0; i < rest.getLength(); i++) {
			removeBlankText(rest.item(i));
		}
	}
}
